package org.jobrunr.dashboard.ui.model;

import org.jobrunr.jobs.RecurringJob;

import java.time.Instant;

public class RecurringJobUIModel extends RecurringJob {

    private Instant nextRun;

    public RecurringJobUIModel(RecurringJob recurringJob) {
        super(recurringJob.getId(), recurringJob.getJobDetails(), recurringJob.getScheduleExpression(), recurringJob.getZoneId(), recurringJob.getCreatedAt().toString());
        setJobName(recurringJob.getJobName());
    }

    @Override
    public Instant getNextRun(Instant currentInstant) {
        nextRun = super.getNextRun(currentInstant);
        return nextRun;
    }
}
