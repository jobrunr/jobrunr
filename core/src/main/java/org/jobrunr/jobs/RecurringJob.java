package org.jobrunr.jobs;

import org.jobrunr.jobs.states.EnqueuedState;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.scheduling.cron.CronExpression;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RecurringJob extends AbstractJob {

    private String id;
    private String cronExpression;
    private String zoneId;

    private RecurringJob() {
        // used for deserialization
    }

    public RecurringJob(String id, JobDetails jobDetails, CronExpression cronExpression, ZoneId zoneId) {
        this(id, jobDetails, cronExpression.getExpression(), zoneId.getId());
    }

    public RecurringJob(String id, JobDetails jobDetails, String cronExpression, String zoneId) {
        super(jobDetails);
        this.id = validateAndSetId(id);
        this.cronExpression = cronExpression;
        this.zoneId = zoneId;
        validateCronExpression();
    }

    @Override
    public String getId() {
        return id;
    }

    public String getCronExpression() {
        return cronExpression;
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

    public Instant getNextRun() {
        return getNextRun(Instant.now());
    }

    public Instant getNextRun(Instant sinceInstant) {
        return CronExpression.create(cronExpression).next(sinceInstant, ZoneId.of(zoneId));
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

    private void validateCronExpression() {
        Instant base = Instant.EPOCH;
        Instant fiveSeconds = base.plusSeconds(5);
        if (CronExpression.create(cronExpression).next(base, ZoneOffset.UTC).isBefore(fiveSeconds)) {
            throw new IllegalArgumentException("The smallest interval for recurring jobs is 5 seconds. Please also make sure that your 'pollIntervalInSeconds' configuration matches the smallest recurring job interval.");
        }
    }


    private String validateAndSetId(String input) {
        String result = Optional.ofNullable(input).orElse(getJobSignature().replace("$", "_")); //why: to support inner classes

        if (!result.matches("[\\dA-Za-z-_(),.]+")) {
            throw new IllegalArgumentException("The id of a recurring job can only contain letters and numbers.");
        }
        return result;
    }

    private Job toJob(JobState jobState) {
        final Job job = new Job(getJobDetails(), jobState);
        job.setJobName(getJobName());
        return job;
    }
}
