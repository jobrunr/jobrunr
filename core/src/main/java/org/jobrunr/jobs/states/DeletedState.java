package org.jobrunr.jobs.states;

import java.time.Instant;

@SuppressWarnings("FieldMayBeFinal") // because of JSON-B
public class DeletedState extends AbstractJobState {

    private String reason;

    protected DeletedState() { // for json deserialization
        this(null);
    }

    public DeletedState(String reason) {
        this(Instant.now(), reason);
    }

    public DeletedState(Instant createdAt, String reason) {
        super(createdAt, StateName.DELETED);
        this.reason = reason;
    }

    public Instant getDeletedAt() {
        return getCreatedAt();
    }

    public String getReason() {
        return reason;
    }
}
