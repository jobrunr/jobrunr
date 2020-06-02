package org.jobrunr.jobs.states;

import java.time.Instant;
import java.util.UUID;

public class ProcessingState extends AbstractJobState {

    private final UUID serverId;
    private Instant updatedAt;

    private ProcessingState() { // for jackson deserialization
        this(null);
    }

    public ProcessingState(UUID serverId) {
        super(StateName.PROCESSING);
        this.serverId = serverId;
        this.updatedAt = getCreatedAt();
    }

    public UUID getServerId() {
        return serverId;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
