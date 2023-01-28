package org.jobrunr.jobs;

import org.jobrunr.jobs.states.EnqueuedState;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.scheduling.Schedule;
import org.jobrunr.scheduling.ScheduleExpressionType;
import org.jobrunr.utils.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RecurringJob extends AbstractJob {

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
        schedule.validateSchedule();
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

    /**
     * Returns the next job to for this recurring job based on the current instant.
     * @return the next job to for this recurring job based on the current instant.
     */
    public Job toScheduledJob() {
        return toJob(new ScheduledState(getNextRun(), this));
    }

    /**
     * Creates all jobs that must be scheduled between the given start and end time.
     *
     * @param from the start time from which to create Scheduled Jobs
     * @param upTo the end time until which to create Scheduled Jobs
     * @return creates all jobs that must be scheduled
     */
    public List<Job> toScheduledJobs(Instant from, Instant upTo) {
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

    public Instant getCreatedAt() {
        return createdAt;
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

        if(result.length() >= 128 && input == null) {
            //why: id's should be identical for identical recurring jobs as otherwise we would generate duplicate recurring jobs after restarts
            result = StringUtils.md5Checksum(result);
        } else if(result.length() >= 128) {
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
}
