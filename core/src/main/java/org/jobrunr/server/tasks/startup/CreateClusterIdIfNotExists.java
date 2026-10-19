package org.jobrunr.server.tasks.startup;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.JarUtils;

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
            storageProvider.saveMetadata(new JobRunrMetadata("database_version", "cluster", JarUtils.getVersion(JobRunr.class)));
        }
    }
}
