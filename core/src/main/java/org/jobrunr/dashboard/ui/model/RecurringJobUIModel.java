package org.jobrunr.dashboard.ui.model;

import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.scheduling.TemporalWrapper;

public class RecurringJobUIModel extends RecurringJob {

    private final TemporalWrapper nextRun;
    private final String scheduleExpression;

    public RecurringJobUIModel(RecurringJob recurringJob) {
        super(recurringJob.getId(), recurringJob.getJobDetails(), recurringJob.getScheduleAsString(), recurringJob.getZoneId(), recurringJob.getCreatedAt().toString());
        setJobName(recurringJob.getJobName());
        setLabels(recurringJob.getLabels());
        nextRun = super.getNextRun();
        scheduleExpression = super.getScheduleAsString();
    }

    @Override
    public TemporalWrapper getNextRun() {
        return nextRun;
    }
}
