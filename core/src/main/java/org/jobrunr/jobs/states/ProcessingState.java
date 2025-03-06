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
        this(serverId, serverName, null);
    }

    public ProcessingState(UUID serverId, String serverName, Instant createdAt) {
        super(StateName.PROCESSING, createdAt);
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
