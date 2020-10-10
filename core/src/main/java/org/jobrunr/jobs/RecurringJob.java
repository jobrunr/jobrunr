package org.jobrunr.jobs;

import org.jobrunr.jobs.states.EnqueuedState;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.scheduling.cron.CronExpression;

import java.time.Instant;
import java.time.ZoneId;
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
        String result = Optional.ofNullable(input).orElse(getJobSignature());

        if (!result.matches("[\\dA-Za-z-_(),.]+")) {
            throw new IllegalArgumentException("The id of a recurring job can only contain letters and numbers.");
        }
        return result;
    }
}
