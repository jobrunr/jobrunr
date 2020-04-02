package org.jobrunr.storage;

import java.util.UUID;

public class ConcurrentJobModificationException extends StorageException {

    public ConcurrentJobModificationException(UUID jobId) {
        super(jobId.toString());
    }

    public ConcurrentJobModificationException(String message) {
        super(message);
    }
}
