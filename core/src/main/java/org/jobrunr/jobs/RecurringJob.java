package org.jobrunr.jobs;

import org.jobrunr.jobs.states.EnqueuedState;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.scheduling.cron.CronExpression;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
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

    public Job toScheduledJob() {
        Instant nextRun = getNextRun();
        final Job job = new Job(getJobDetails(), new ScheduledState(nextRun, this));
        job.setJobName(getJobName());
        return job;
    }

    public Job toEnqueuedJob() {
        final Job job = new Job(getJobDetails(), new EnqueuedState());
        job.setJobName(getJobName());
        return job;
    }

    public String getZoneId() {
        return zoneId;
    }

    public Instant getNextRun() {
        return CronExpression.create(cronExpression).next(ZoneId.of(zoneId));
    }

    private String validateAndSetId(String input) {
        String result = Optional.ofNullable(input).orElse(getJobSignature().replace("$", "_")); //why: to support inner classes

        if (!result.matches("[\\dA-Za-z-_(),.]+")) {
            throw new IllegalArgumentException("The id of a recurring job can only contain letters and numbers.");
        }
        return result;
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
        Instant five_seconds = base.plusSeconds(5);
        if (CronExpression.create(cronExpression).next(base, ZoneOffset.UTC).isBefore(five_seconds)) {
            throw new IllegalArgumentException("The smallest interval for recurring jobs is 5 seconds. Please also make sure that your 'pollIntervalInSeconds' configuration matches the smallest recurring job interval.");
        }
    }
}
