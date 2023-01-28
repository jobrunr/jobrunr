package org.jobrunr.dashboard.ui.model;

import org.jobrunr.jobs.RecurringJob;

import java.time.Instant;

public class RecurringJobUIModel extends RecurringJob {

    private final Instant nextRun;

    public RecurringJobUIModel(RecurringJob recurringJob) {
        super(recurringJob.getId(), recurringJob.getJobDetails(), recurringJob.getScheduleExpression(), recurringJob.getZoneId(), recurringJob.getCreatedAt().toString());
        setJobName(recurringJob.getJobName());
        setLabels(recurringJob.getLabels());
        nextRun = super.getNextRun();
    }

    @Override
    public Instant getNextRun() {
        return nextRun;
    }
}
