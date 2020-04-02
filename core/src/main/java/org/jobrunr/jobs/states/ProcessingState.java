package org.jobrunr.jobs.states;

import java.util.UUID;

public class ProcessingState extends AbstractJobState {

    private final UUID serverId;

    private ProcessingState() { // for jackson deserialization
        this(null);
    }

    public ProcessingState(UUID serverId) {
        super(StateName.PROCESSING);
        this.serverId = serverId;
    }

    public UUID getServerId() {
        return serverId;
    }
}
