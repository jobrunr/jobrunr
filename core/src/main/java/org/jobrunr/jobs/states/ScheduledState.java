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
        this(scheduledAt, reason, null);
    }

    protected ScheduledState(Instant scheduledAt, String reason, String recurringJobId) {
        this(scheduledAt, reason, recurringJobId, Instant.now());
    }

    public ScheduledState(Instant scheduledAt, String reason, String recurringJobId, Instant createdAt) {
        super(StateName.SCHEDULED, createdAt);
        this.scheduledAt = scheduledAt;
        this.reason = reason;
        this.recurringJobId = recurringJobId;
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

    public static ScheduledState fromRecurringJob(Instant scheduledAt, RecurringJob recurringJob) {
        return fromRecurringJob(scheduledAt, recurringJob, "Scheduled by recurring job '" + recurringJob.getJobName() + "'");
    }

    public static ScheduledState fromRecurringJobAheadOfTime(Instant scheduledAt, RecurringJob recurringJob) {
        return fromRecurringJob(scheduledAt, recurringJob, "Scheduled ahead of time by recurring job '" + recurringJob.getJobName() + "'");
    }

    private static ScheduledState fromRecurringJob(Instant scheduledAt, RecurringJob recurringJob, String reason) {
        ScheduledState scheduledState = new ScheduledState(scheduledAt, reason);
        scheduledState.recurringJobId = recurringJob.getId();
        return scheduledState;
    }
}
