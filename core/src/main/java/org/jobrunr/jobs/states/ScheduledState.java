package org.jobrunr.jobs.states;

import java.time.Instant;

public class ScheduledState extends AbstractJobState {

    private final Instant scheduledAt;
    private final String reason;

    private ScheduledState() { // for jackson deserialization
        this(null);
    }

    public ScheduledState(Instant scheduledAt) {
        this(scheduledAt, null);
    }

    public ScheduledState(Instant scheduledAt, String reason) {
        super(StateName.SCHEDULED);
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
