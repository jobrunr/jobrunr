package org.jobrunr.jobs;

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
        super(jobDetails);
        this.id = Optional.ofNullable(id).orElse(getJobSignature());
        this.cronExpression = cronExpression.getExpression();
        this.zoneId = zoneId.getId();
    }

    public String getId() {
        return id;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public Job toScheduledJob() {
        Instant nextRun = getNextRun();
        return new Job(getJobDetails(), new ScheduledState(nextRun));
    }

    public Instant getNextRun() {
        return CronExpression.create(cronExpression).next(ZoneId.of(zoneId));
    }
}
