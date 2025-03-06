package org.jobrunr.jobs.states;

import java.time.Instant;

@SuppressWarnings("FieldMayBeFinal") // because of JSON-B
public class DeletedState extends AbstractJobState {

    private String reason;

    protected DeletedState() { // for json deserialization
        this(null);
    }

    public DeletedState(String reason) {
        this(reason, null);
    }

    public DeletedState(String reason, Instant createdAt) {
        super(StateName.DELETED, createdAt);
        this.reason = reason;
    }

    public Instant getDeletedAt() {
        return getCreatedAt();
    }

    public String getReason() {
        return reason;
    }
}
