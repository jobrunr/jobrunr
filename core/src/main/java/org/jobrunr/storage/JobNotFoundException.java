package org.jobrunr.storage;

import java.util.UUID;

public class JobNotFoundException extends StorageException {

    public JobNotFoundException(UUID jobId) {
        super(String.format("Job with id %s does not exist", jobId));
    }

}
