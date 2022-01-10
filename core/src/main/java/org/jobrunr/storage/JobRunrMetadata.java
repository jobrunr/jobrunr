package org.jobrunr.storage;

import java.time.Instant;
import java.util.UUID;

public class JobRunrMetadata {

    private final String name;
    private final String owner;
    private final Instant createdAt;
    private final Instant updatedAt;
    private String metadata;

    public JobRunrMetadata(String name, String owner, Object metadata) {
        this(name, owner, metadata.toString());
    }

    public JobRunrMetadata(String name, String owner, String metadata) {
        this(name, owner, metadata, Instant.now(), Instant.now());
    }

    public JobRunrMetadata(String name, String owner, String metadata, Instant createdAt, Instant updatedAt) {
        this.name = name;
        this.owner = owner;
        this.metadata = metadata;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return toId(name, owner);
    }

    public String getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getMetadata() {
        return metadata;
    }

    public Long getValueAsLong() {
        return Long.parseLong(metadata);
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public static String toId(String name, String owner) {
        return name.replace(" ", "_") + "-" + owner.replace(" ", "_");
    }

    public static String toId(String name, UUID owner) {
        return toId(name, owner.toString());
    }
}
