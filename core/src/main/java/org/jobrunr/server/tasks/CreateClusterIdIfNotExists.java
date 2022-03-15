package org.jobrunr.server.tasks;

import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.StorageProvider;

import java.util.UUID;

public class CreateClusterIdIfNotExists implements Runnable {

    private final StorageProvider storageProvider;

    public CreateClusterIdIfNotExists(BackgroundJobServer backgroundJobServer) {
        storageProvider = backgroundJobServer.getStorageProvider();
    }

    @Override
    public void run() {
        JobRunrMetadata metadata = storageProvider.getMetadata("id", "cluster");
        if (metadata == null) {
            storageProvider.saveMetadata(new JobRunrMetadata("id", "cluster", UUID.randomUUID().toString()));
        }
    }
}
