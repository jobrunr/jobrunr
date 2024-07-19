package org.jobrunr.jobs;

import org.jobrunr.jobs.states.EnqueuedState;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.scheduling.Schedule;
import org.jobrunr.scheduling.ScheduleExpressionType;
import org.jobrunr.storage.StorageProviderUtils;
import org.jobrunr.utils.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Collections.emptyList;

public class RecurringJob extends AbstractJob {

    public static Map<String, Function<RecurringJob, Comparable>> ALLOWED_SORT_COLUMNS = new HashMap<>();

    static {
        ALLOWED_SORT_COLUMNS.put(StorageProviderUtils.RecurringJobs.FIELD_ID, RecurringJob::getId);
        ALLOWED_SORT_COLUMNS.put(StorageProviderUtils.RecurringJobs.FIELD_CREATED_AT, RecurringJob::getCreatedAt);
    }

    private String id;
    private String scheduleExpression;
    private String zoneId;
    private Instant createdAt;

    private RecurringJob() {
        // used for deserialization
    }

    public RecurringJob(String id, JobDetails jobDetails, String scheduleExpression, String zoneId) {
        this(id, jobDetails, ScheduleExpressionType.getSchedule(scheduleExpression), ZoneId.of(zoneId));
    }

    public RecurringJob(String id, JobDetails jobDetails, Schedule schedule, ZoneId zoneId) {
        this(id, jobDetails, schedule, zoneId, Instant.now(Clock.system(zoneId)));
    }

    public RecurringJob(String id, JobDetails jobDetails, String scheduleExpression, String zoneId, String createdAt) {
        this(id, jobDetails, ScheduleExpressionType.getSchedule(scheduleExpression), ZoneId.of(zoneId), Instant.parse(createdAt));
    }

    public RecurringJob(String id, JobDetails jobDetails, Schedule schedule, ZoneId zoneId, Instant createdAt) {
        super(jobDetails);
        this.id = validateAndSetId(id);
        this.zoneId = zoneId.getId();
        this.scheduleExpression = schedule.toString();
        this.createdAt = createdAt;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getScheduleExpression() {
        return scheduleExpression;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getZoneId() {
        return zoneId;
    }

    /**
     * Creates all jobs that must be scheduled in the time interval [from, upTo), with one additional job scheduled ahead of time.
     *
     * @param from the start of the time interval from which to create Scheduled Jobs
     * @param upTo the end of the time interval (not included)
     * @return all jobs that must be scheduled in the time interval [from, upTo), with one additional job scheduled ahead of time.
     */
    public List<Job> toJobsWith1FutureRun(Instant from, Instant upTo) {
        if (from.isAfter(upTo)) return emptyList();

        List<Job> jobs = new ArrayList<>();

        Schedule schedule = ScheduleExpressionType.getSchedule(scheduleExpression);
        ZoneId zoneId = ZoneId.of(this.zoneId);

        Instant nextRun = schedule.next(createdAt, from, zoneId);
        while (nextRun.isBefore(upTo)) {
            jobs.add(toJob(toJobState(schedule, nextRun)));
            nextRun = schedule.next(createdAt, nextRun, zoneId);
        }

        // add 1 more job
        jobs.add(toJob(toJobState(schedule, nextRun)));

        return jobs;
    }

    public Job toEnqueuedJob() {
        return toJob(new EnqueuedState());
    }

    public Instant getNextRun() {
        return getNextRun(Instant.now());
    }

    public Instant getNextRun(Instant sinceInstant) {
        return ScheduleExpressionType
                .getSchedule(scheduleExpression)
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

    private JobState toJobState(Schedule schedule, Instant scheduleAt) {
        if (schedule.isCarbonAware()) {
            return schedule.getCarbonAwareScheduleMargin().toCarbonAwareAwaitingState(scheduleAt);
        }
        return new ScheduledState(scheduleAt, this);
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


