package org.jobrunr.jobs.states;

import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.BackgroundJobServerConfigurationReader;

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
        this(backgroundJobServer.getConfiguration());
    }

    public ProcessingState(BackgroundJobServerConfigurationReader backgroundJobServerConfiguration) {
        this(backgroundJobServerConfiguration.getId(), backgroundJobServerConfiguration.getName());
    }

    public ProcessingState(UUID serverId, String serverName) {
        this(Instant.now(), serverId, serverName);
    }

    protected ProcessingState(Instant createdAt, UUID serverId, String serverName) {
        this(createdAt, createdAt, serverId, serverName);
    }

    public ProcessingState(Instant createdAt, Instant updatedAt, UUID serverId, String serverName) {
        super(createdAt, StateName.PROCESSING);
        this.serverId = serverId;
        this.serverName = serverName;
        this.updatedAt = updatedAt;
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
