package org.jobrunr.server.tasks;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.PageRequest;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.SqlStorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toList;
import static org.jobrunr.storage.PageRequest.ascOnUpdatedAt;

public class MigrateFromV5toV6Task implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MigrateFromV5toV6Task.class);

    private final StorageProvider storageProvider;

    public MigrateFromV5toV6Task(BackgroundJobServer backgroundJobServer) {
        storageProvider = backgroundJobServer.getStorageProvider();
    }

    @Override
    public void run() {
        JobRunrMetadata metadata = storageProvider.getMetadata("database_version", "cluster");
        if(metadata != null && "6.0.0".equals(metadata.getValue())) return;

        migrateScheduledJobsIfNecessary();

        storageProvider.saveMetadata(new JobRunrMetadata("database_version", "cluster", "6.0.0"));
    }

    private void migrateScheduledJobsIfNecessary() {
        if(!(SqlStorageProvider.class.isAssignableFrom(storageProvider.getStorageProviderInfo().getImplementationClass()))) {
            LOGGER.info("Migration of scheduled jobs from v5 to v6 not needed as not using an SqlStorageProvider");
            return;
        }

        LOGGER.info("Start migration of scheduled jobs from v5 to v6");
        try {
            List<UUID> scheduledJobIdsToMigrate = getScheduledJobIdsToMigrate();
            LOGGER.info("Found {} scheduled jobs to migrate.", scheduledJobIdsToMigrate.size());
            List<Job> scheduledJobsToMigrate = new ArrayList<>();
            for (UUID jobId : scheduledJobIdsToMigrate) {
                Job scheduledJob = storageProvider.getJobById(jobId);
                scheduledJobsToMigrate.add(scheduledJob);

                if(scheduledJobsToMigrate.size() >= 1000) {
                    storageProvider.save(scheduledJobsToMigrate);
                    scheduledJobsToMigrate.clear();
                }
            }

            if(!scheduledJobsToMigrate.isEmpty()) {
                storageProvider.save(scheduledJobsToMigrate);
                scheduledJobsToMigrate.clear();
            }
            LOGGER.info("Finished migration of scheduled jobs from v5 to v6");
        } catch (Exception e) {
            LOGGER.error("Error migrating scheduled jobs from v5 to v6.", e);
            throw e;
        }
    }

    private List<UUID> getScheduledJobIdsToMigrate() {
        PageRequest pageRequest = ascOnUpdatedAt(5000);
        final List<UUID> allScheduledJobsId = new ArrayList<>();
        List<Job> scheduledJobs = this.storageProvider.getScheduledJobs(Instant.parse("2100-01-01T00:00:00Z"), pageRequest);
        while(!scheduledJobs.isEmpty()) {
            allScheduledJobsId.addAll(scheduledJobs.stream().map(Job::getId).collect(toList()));
            pageRequest = pageRequest.nextPage();
            scheduledJobs = this.storageProvider.getScheduledJobs(Instant.parse("2100-01-01T00:00:00Z"), pageRequest);
        }
        return allScheduledJobsId;
    }
}
