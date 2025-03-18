package org.jobrunr.jobs;

import org.jobrunr.jobs.states.EnqueuedState;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.scheduling.Schedule;
import org.jobrunr.scheduling.ScheduleExpressionType;
import org.jobrunr.storage.StorageProviderUtils;
import org.jobrunr.utils.StringUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.time.Duration.between;

public class RecurringJob extends AbstractJob {

    public enum CreatedBy {
        API,
        ANNOTATION
    }

    public static Map<String, Function<RecurringJob, Comparable>> ALLOWED_SORT_COLUMNS = new HashMap<>();

    static {
        ALLOWED_SORT_COLUMNS.put(StorageProviderUtils.RecurringJobs.FIELD_ID, RecurringJob::getId);
        ALLOWED_SORT_COLUMNS.put(StorageProviderUtils.RecurringJobs.FIELD_CREATED_AT, RecurringJob::getCreatedAt);
    }

    private String id;
    private String scheduleExpression;
    private String zoneId;
    private CreatedBy createdBy;
    private Instant createdAt;

    private RecurringJob() {
        // used for deserialization
    }

    public RecurringJob(String id, JobDetails jobDetails, String scheduleExpression, String zoneId, CreatedBy createdBy) {
        this(id, jobDetails, ScheduleExpressionType.createScheduleFromString(scheduleExpression), ZoneId.of(zoneId), createdBy);
    }

    public RecurringJob(String id, JobDetails jobDetails, Schedule schedule, ZoneId zoneId, CreatedBy createdBy) {
        this(id, jobDetails, schedule, zoneId, createdBy, Instant.now(Clock.system(zoneId)));
    }

    public RecurringJob(String id, JobDetails jobDetails, String scheduleExpression, String zoneId, CreatedBy createdBy, String createdAt) {
        this(id, jobDetails, ScheduleExpressionType.createScheduleFromString(scheduleExpression), ZoneId.of(zoneId), createdBy, Instant.parse(createdAt));
    }

    public RecurringJob(String id, JobDetails jobDetails, Schedule schedule, ZoneId zoneId, CreatedBy createdBy, Instant createdAt) {
        super(jobDetails);
        this.id = validateAndSetId(id);
        this.zoneId = zoneId.getId();
        this.scheduleExpression = schedule.toString();
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getScheduleExpression() {
        return scheduleExpression;
    }

    /**
     * Returns the next job to schedule for this recurring job based on the current instant.
     *
     * @return the next job to schedule for this recurring job based on the current instant.
     */
    @Deprecated
    public Job toScheduledJob() {
        return toJob(new ScheduledState(getNextRun(), this));
    }

    /**
     * Returns the next job to schedule for this recurring job based on the given instant.
     *
     * @return the next job to schedule for this recurring job based on the given instant
     */
    public Job toScheduledJobAheadOfTime(Instant after) {
        return toJob(new ScheduledState(getNextRun(after), this));
    }

    /**
     * Creates all jobs that must be scheduled in the time interval [from, upTo).
     *
     * @param from the start of the time interval from which to create Scheduled Jobs
     * @param upTo the end of the time interval (not included)
     * @return all jobs that must be scheduled in the time interval [from, upTo)
     */
    public List<Job> toScheduledJobs(Instant from, Instant upTo) {
        if (from.isAfter(upTo)) {
            throw new IllegalArgumentException("from must be after upTo");
        }

        List<Job> jobs = new ArrayList<>();
        Instant nextRun = getNextRun(from);
        while (nextRun.isBefore(upTo)) {
            jobs.add(toJob(new ScheduledState(nextRun, this)));
            nextRun = getNextRun(nextRun);
        }

        return jobs;
    }

    public Job toEnqueuedJob() {
        return toJob(new EnqueuedState());
    }

    public String getZoneId() {
        return zoneId;
    }

    public CreatedBy getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getNextRun() {
        return getNextRun(Instant.now());
    }

    public Instant getNextRun(Instant sinceInstant) {
        return ScheduleExpressionType
                .createScheduleFromString(scheduleExpression)
                .next(createdAt, sinceInstant, ZoneId.of(zoneId));
    }

    private String validateAndSetId(String input) {
        String result = Optional.ofNullable(input).orElse(getJobSignature().replace(" ", "").replace("$", "_")); //why: to support inner classes

        if (result.length() >= 128 && input == null) {
            //why: id's should be identical for identical recurring jobs as otherwise we would generate duplicate recurring jobs after restarts
            result = StringUtils.md5Checksum(result);
        } else if (result.length() >= 128) {
            throw new IllegalArgumentException("The id of a recurring job must be smaller than 128 characters.");
        } else if (!result.matches("[\\dA-Za-z-_(),.]+")) {
            throw new IllegalArgumentException("The id of a recurring job can only contain letters and numbers.");
        }

        return result;
    }

    private Job toJob(JobState jobState) {
        final Job job = new Job(getJobDetails(), jobState);
        job.setJobName(getJobName());
        job.setRecurringJobId(getId());
        job.setAmountOfRetries(getAmountOfRetries());
        job.setLabels(getLabels());
        return job;
    }

    @Override
    public String toString() {
        return "RecurringJob{" +
                "id=" + id +
                ", version='" + getVersion() + '\'' +
                ", identity='" + System.identityHashCode(this) + '\'' +
                ", jobSignature='" + getJobSignature() + '\'' +
                ", jobName='" + getJobName() + '\'' +
                '}';
    }

    public Duration durationBetweenRecurringJobInstances() {
        Instant base = Instant.EPOCH.plusSeconds(3600);
        Schedule schedule = ScheduleExpressionType.createScheduleFromString(scheduleExpression);
        Instant run1 = schedule.next(base, base, ZoneOffset.UTC);
        Instant run2 = schedule.next(base, run1, ZoneOffset.UTC);
        return between(run1, run2);
    }
}
