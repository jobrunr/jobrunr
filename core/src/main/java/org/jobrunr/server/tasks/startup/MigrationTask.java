package org.jobrunr.server.tasks.startup;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.StorageProvider;

import static org.jobrunr.utils.VersionNumber.v;

public abstract class MigrationTask implements Runnable {

    protected final StorageProvider storageProvider;
    private final String version;

    public MigrationTask(BackgroundJobServer backgroundJobServer, String version) {
        this.storageProvider = backgroundJobServer.getStorageProvider();
        this.version = version;
    }

    @Override
    public void run() {
        if (hasMigrationAlreadyRun()) return;
        runMigration();
        markMigrationAsCompleted();
    }

    protected abstract void runMigration();

    private boolean hasMigrationAlreadyRun() {
        if (JobRunr.VERSION.isV1Snapshot()) return true;

        JobRunrMetadata metadata = storageProvider.getMetadata("database_version", "cluster");
        return (metadata != null && v(metadata.getValue()).isNewerOrEqualTo(v(version)));
    }

    private void markMigrationAsCompleted() {
        storageProvider.saveMetadata(new JobRunrMetadata("database_version", "cluster", version));
    }
}
