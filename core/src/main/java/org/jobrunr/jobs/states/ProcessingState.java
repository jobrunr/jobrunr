package org.jobrunr.jobs.states;

import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.BackgroundJobServerStatus;

import java.time.Instant;
import java.util.UUID;

@SuppressWarnings("FieldMayBeFinal") // because of JSON-B
public class ProcessingState extends AbstractJobState {

    private UUID serverId;
    private String serverName;
    private Instant updatedAt;

    protected ProcessingState() { // for json deserialization
        this(null, null);
    }

    public ProcessingState(BackgroundJobServer backgroundJobServer) {
        this(backgroundJobServer.getServerStatus());
    }

    public ProcessingState(BackgroundJobServerStatus backgroundJobServerStatus) {
        this(backgroundJobServerStatus.getId(), backgroundJobServerStatus.getName());
    }

    public ProcessingState(UUID serverId, String serverName) {
        super(StateName.PROCESSING);
        this.serverId = serverId;
        this.serverName = serverName;
        this.updatedAt = getCreatedAt();
    }

    public UUID getServerId() {
        return serverId;
    }

    public String getServerName() {
        return serverName;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
