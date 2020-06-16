package org.jobrunr.jobs.states;

import java.time.Instant;

@SuppressWarnings("FieldMayBeFinal") // because of JSON-B
public class ScheduledState extends AbstractJobState {

    private Instant scheduledAt;
    private String reason;

    protected ScheduledState() { // for json deserialization
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
