package org.jobrunr.server.tasks.startup;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.JarUtils;

/**
 * Startup Task that keeps DB Version in sync with the actual JobRunr version.
 */
public class SaveCurrentVersionTask implements Runnable {

    private final StorageProvider storageProvider;

    public SaveCurrentVersionTask(BackgroundJobServer backgroundJobServer) {
        storageProvider = backgroundJobServer.getStorageProvider();
    }

    @Override
    public void run() {
        storageProvider.saveMetadata(new JobRunrMetadata("database_version", "cluster", JarUtils.getVersion(JobRunr.class)));
    }

}
