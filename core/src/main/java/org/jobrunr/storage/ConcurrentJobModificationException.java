package org.jobrunr.storage;

import java.util.UUID;

public class ConcurrentJobModificationException extends StorageException {

    private UUID jobId;

    public ConcurrentJobModificationException(UUID jobId) {
        super(jobId.toString());
        this.jobId = jobId;
    }

    public ConcurrentJobModificationException(String message) {
        super(message);
    }

    public UUID getJobId() {
        return jobId;
    }
}
