package org.jobrunr.storage;

import java.util.UUID;

public class JobNotFoundException extends StorageException {

    public JobNotFoundException(UUID jobId) {
        this(jobId.toString());
    }

    public JobNotFoundException(String jobId) {
        super(String.format("Job with id %s does not exist", jobId));
    }

}
