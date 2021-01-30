package org.jobrunr.storage;

import java.time.Instant;

public class JobRunrMetadata {

    private String name;
    private String owner;
    private String value;
    private Instant createdAt;
    private Instant updatedAt;

    public JobRunrMetadata(String name, String owner, String value) {
        this(name, owner, value, Instant.now(), Instant.now());
    }

    public JobRunrMetadata(String name, String owner, String value, Instant createdAt, Instant updatedAt) {
        this.name = name;
        this.owner = owner;
        this.value = value;
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

    public String getValue() {
        return value;
    }

    public Long getValueAsLong() {
        return Long.parseLong(value);
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public static String toId(String name, String owner) {
        return name + "-" + owner;
    }
}