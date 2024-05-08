package org.jobrunr.dashboard.ui.model;

import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.scheduling.TemporalWrapper;

public class RecurringJobUIModel extends RecurringJob {

    private final TemporalWrapper nextRun;
    private final String scheduleExpression;

    public RecurringJobUIModel(RecurringJob recurringJob) {
        super(recurringJob.getId(), recurringJob.getJobDetails(), recurringJob.getSchedule().toString(), recurringJob.getZoneId(), recurringJob.getCreatedAt().toString());
        setJobName(recurringJob.getJobName());
        setLabels(recurringJob.getLabels());
        nextRun = super.getNextRun();
        scheduleExpression = super.getSchedule().toString();
    }

    @Override
    public TemporalWrapper getNextRun() {
        return nextRun;
    }
}
