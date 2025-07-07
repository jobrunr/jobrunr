package org.jobrunr.jobs.states;

import org.jobrunr.jobs.RecurringJob;

import java.time.Instant;

import static java.time.Instant.now;

@SuppressWarnings("FieldMayBeFinal") // because of JSON-B
public class ScheduledState extends AbstractJobState implements SchedulableState {

    private Instant scheduledAt;
    private String reason;

    protected ScheduledState() { // for json deserialization
        this(null);
    }

    public ScheduledState(Instant scheduledAt) {
        this(scheduledAt, (String) null);
    }

    public ScheduledState(Instant scheduledAt, RecurringJob recurringJob) {
        this(scheduledAt, "Scheduled by recurring job '" + recurringJob.getJobName() + "'");
    }

    public ScheduledState(Instant scheduledAt, String reason) {
        this(scheduledAt, reason, now());
    }

    public ScheduledState(Instant scheduledAt, String reason, Instant createdAt) {
        super(StateName.SCHEDULED, createdAt);
        this.scheduledAt = scheduledAt;
        this.reason = reason;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public String getReason() {
        return reason;
    }

}
