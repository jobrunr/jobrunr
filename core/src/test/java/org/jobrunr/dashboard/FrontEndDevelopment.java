package org.jobrunr.dashboard;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.SimpleStorageProvider;
import org.jobrunr.storage.StorageProvider;

import static java.time.Instant.now;
import static org.jobrunr.jobs.JobTestBuilder.aFailedJobThatEventuallySucceeded;
import static org.jobrunr.jobs.JobTestBuilder.aFailedJobWithRetries;
import static org.jobrunr.jobs.JobTestBuilder.aJob;
import static org.jobrunr.jobs.JobTestBuilder.aSucceededJob;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;

public class FrontEndDevelopment {

    public static void main(String[] args) throws InterruptedException {
        StorageProvider storageProvider = new SimpleStorageProvider();

        final BackgroundJobServerStatus backgroundJobServerStatus = new BackgroundJobServerStatus(10, 10);
        backgroundJobServerStatus.start();
        storageProvider.announceBackgroundJobServer(backgroundJobServerStatus);
        for (int i = 0; i < 33; i++) {
            storageProvider.save(anEnqueuedJob().build());
        }
        storageProvider.save(aJob().withState(new ScheduledState(now().plusSeconds(60 * 60 * 5))).build());
        storageProvider.save(aSucceededJob().build());
        storageProvider.save(aFailedJobWithRetries().build());
        storageProvider.save(aFailedJobThatEventuallySucceeded().build());

        JobRunr
                .configure()
                .useJobStorageProvider(storageProvider)
                .useDashboard()
                .initialize();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Thread.currentThread().interrupt();
        }));

        Thread.currentThread().join();
    }
}
