package org.jobrunr.dashboard.ui.model;

import org.jobrunr.jobs.RecurringJob;

import java.time.Instant;

public class RecurringJobUIModel extends RecurringJob {

    private Instant nextRun;

    public RecurringJobUIModel(RecurringJob recurringJob) {
        super(recurringJob.getId(), recurringJob.getJobDetails(), recurringJob.getCronExpression(), recurringJob.getZoneId());
        nextRun = super.getNextRun();
    }

    @Override
    public Instant getNextRun() {
        return nextRun;
    }
}
