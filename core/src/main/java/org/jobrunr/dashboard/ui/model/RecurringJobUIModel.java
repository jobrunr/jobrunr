package org.jobrunr.dashboard.ui.model;

import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.scheduling.RecurringJobNextRun;

public class RecurringJobUIModel extends RecurringJob {

    private final RecurringJobNextRun nextRun;

    public RecurringJobUIModel(RecurringJob recurringJob) {
        super(recurringJob.getId(), recurringJob.getJobDetails(), recurringJob.getScheduleExpression(), recurringJob.getZoneId(), recurringJob.getCreatedAt().toString());
        setJobName(recurringJob.getJobName());
        setLabels(recurringJob.getLabels());
        nextRun = super.getNextRun();
    }

    @Override
    public RecurringJobNextRun getNextRun() {
        return nextRun;
    }
}
