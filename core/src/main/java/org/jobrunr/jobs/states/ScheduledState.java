package org.jobrunr.jobs.states;

import org.jobrunr.jobs.RecurringJob;

import java.time.Instant;

@SuppressWarnings("FieldMayBeFinal") // because of JSON-B
public class ScheduledState extends AbstractJobState {

    private Instant scheduledAt;
    private String recurringJobId;
    private String reason;

    protected ScheduledState() { // for json deserialization
        this(null);
    }

    public ScheduledState(Instant scheduledAt) {
        this(scheduledAt, (String) null);
    }

    public ScheduledState(Instant scheduledAt, RecurringJob recurringJob) {
        this(scheduledAt, "Scheduled by recurring job '" + recurringJob.getJobName() + "'");
        this.recurringJobId = recurringJob.getId();
    }

    public ScheduledState(Instant scheduledAt, String reason) {
        super(StateName.SCHEDULED);
        this.scheduledAt = scheduledAt;
        this.reason = reason;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public String getRecurringJobId() {
        return recurringJobId;
    }

    public String getReason() {
        return reason;
    }
}
