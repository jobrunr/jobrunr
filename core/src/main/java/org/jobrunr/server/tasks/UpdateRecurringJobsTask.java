package org.jobrunr.server.tasks;

import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.RecurringJobsResult;
import org.jobrunr.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This task updates the recurring jobs so that they can be cached properly.
 * It is scheduled for removal in JobRunr 6 as then all users should already have run this task.
 */
@Deprecated
public class UpdateRecurringJobsTask implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackgroundJobServer.class);

    private final StorageProvider storageProvider;

    public UpdateRecurringJobsTask(BackgroundJobServer backgroundJobServer) {
        storageProvider = backgroundJobServer.getStorageProvider();
    }

    @Override
    public void run() {
        RecurringJobsResult recurringJobs = storageProvider.getRecurringJobs();
        if(storageProvider.recurringJobsUpdated(recurringJobs.getLastModifiedHash())) {
            LOGGER.info("Found {} recurring jobs that need to be updated.", recurringJobs.size());
            recurringJobs.forEach(storageProvider::saveRecurringJob);
        }
    }
}
