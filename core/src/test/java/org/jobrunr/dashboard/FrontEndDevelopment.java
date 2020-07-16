package org.jobrunr.dashboard;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StubDataProvider;
import org.jobrunr.storage.sql.h2.InMemoryStorageProvider;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;

/**
 * Main Class to run for FrontEndDevelopment
 */
public class FrontEndDevelopment {

    public static void main(String[] args) throws InterruptedException {
        StorageProvider storageProvider = new InMemoryStorageProvider()
                .withJsonMapper(new JacksonJsonMapper());

        StubDataProvider.using(storageProvider)
                .addALotOfEnqueuedJobsThatTakeSomeTime()
                .addSomeRecurringJobs();

        JobRunr
                .configure()
                .useStorageProvider(storageProvider)
                .useDashboard()
                .useDefaultBackgroundJobServer()
                .initialize();

        //BackgroundJob.<TestService>enqueue(x -> x.doWorkThatTakesLong(JobContext.Null));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> Thread.currentThread().interrupt()));

        Thread.currentThread().join();
    }
}
