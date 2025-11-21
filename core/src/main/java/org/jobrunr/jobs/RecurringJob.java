package org.jobrunr.jobs;

import org.jobrunr.jobs.states.CarbonAwareAwaitingState;
import org.jobrunr.jobs.states.EnqueuedState;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.scheduling.Schedule;
import org.jobrunr.scheduling.ScheduleExpressionType;
import org.jobrunr.storage.StorageProviderUtils;
import org.jobrunr.utils.StringUtils;
import org.jspecify.annotations.Nullable;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

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
    private CreatedBy createdBy = CreatedBy.API;
    private Instant createdAt;

    private transient Schedule schedule;

    private RecurringJob() {
        // used for deserialization
    }

    public RecurringJob(String id, JobDetails jobDetails, String scheduleExpression, String zoneId, CreatedBy createdBy) {
        this(id, jobDetails, ScheduleExpressionType.createScheduleFromString(scheduleExpression), ZoneId.of(zoneId), createdBy);
    }

    public RecurringJob(@Nullable String id, JobDetails jobDetails, Schedule schedule, ZoneId zoneId, CreatedBy createdBy) {
        this(id, jobDetails, schedule, zoneId, createdBy, Instant.now(Clock.system(zoneId)));
    }

    public RecurringJob(String id, JobDetails jobDetails, String scheduleExpression, String zoneId, CreatedBy createdBy, Instant createdAt) {
        this(id, jobDetails, ScheduleExpressionType.createScheduleFromString(scheduleExpression), ZoneId.of(zoneId), createdBy, createdAt);
    }

    public RecurringJob(@Nullable String id, JobDetails jobDetails, Schedule schedule, ZoneId zoneId, CreatedBy createdBy, Instant createdAt) {
        this(id, 0, jobDetails, schedule, zoneId, createdBy, createdAt);
    }

    public RecurringJob(String id, int version, JobDetails jobDetails, String scheduleExpression, String zoneId, CreatedBy createdBy, Instant createdAt) {
        this(id, version, jobDetails, ScheduleExpressionType.createScheduleFromString(scheduleExpression), ZoneId.of(zoneId), createdBy, createdAt);
    }

    public RecurringJob(@Nullable String id, int version, JobDetails jobDetails, Schedule schedule, ZoneId zoneId, CreatedBy createdBy, Instant createdAt) {
        super(jobDetails, version);
        this.id = validateAndSetId(id);
        this.zoneId = zoneId.getId();
        this.schedule = schedule;
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
     * Returns the next job to for this recurring job based on the current instant.
     *
     * @return the next job to for this recurring job based on the current instant.
     */
    public Job toScheduledJob() {
        return toJob(new ScheduledState(getNextRun(), this));
    }

    /**
     * Creates all jobs that must be scheduled in the time interval [from, upTo).
     * Creates a job to schedule ahead of time if no jobs are created in the interval.
     *
     * @param from the start of the time interval from which to create Scheduled Jobs (inclusive)
     * @param upTo the end of the time interval (exclusive)
     * @return all created jobs that must be scheduled in the time interval [from, upTo), or a job scheduled ahead of time.
     */
    public List<Job> toScheduledJobs(Instant from, Instant upTo) {
        if (from.isAfter(upTo)) throw new IllegalArgumentException("from must be before upTo");

        List<Job> jobs = new ArrayList<>();
        Instant nextRun = getNextRun(from);

        // add from inclusive
        if (from.equals(getNextRun(from.minusMillis(1)))) {
            jobs.add(toJob(getNextState(from, "By recurring job '" + getJobName() + "'")));
        }

        while (nextRun.isBefore(upTo)) {
            jobs.add(toJob(getNextState(nextRun, "By recurring job '" + getJobName() + "'")));
            nextRun = getNextRun(nextRun);
        }

        if (jobs.isEmpty()) {
            Instant nextRunAtAheadOfTime = getNextRun(upTo);
            jobs.add(toJob(getNextState(nextRunAtAheadOfTime, "Ahead of time by recurring job '" + getJobName() + "'")));
        }

        return jobs;
    }

    public Schedule getSchedule() {
        if (schedule == null) {
            schedule = ScheduleExpressionType.createScheduleFromString(scheduleExpression);
        }
        return schedule;
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
        return getSchedule().next(createdAt, sinceInstant, ZoneId.of(zoneId));
    }

    private String validateAndSetId(@Nullable String input) {
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

    private JobState getNextState(Instant nextRun, String reason) {
        Schedule schedule = getSchedule();
        return schedule.isCarbonAware()
                ? new CarbonAwareAwaitingState(nextRun, schedule.getCarbonAwareScheduleMargin(), reason)
                : new ScheduledState(nextRun, reason);
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
}
