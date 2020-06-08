package org.jobrunr.dashboard;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.JobContext;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.storage.SimpleStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.stubs.TestService;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;

/**
 * Main Class to run for FrontEndDevelopment
 */
public class FrontEndDevelopment {

    public static void main(String[] args) throws InterruptedException {
        StorageProvider storageProvider = new SimpleStorageProvider()
                .withJsonMapper(new JacksonJsonMapper())
                //.withALotOfEnqueuedJobsThatTakeSomeTime()
                //.withSomeRecurringJobs();
                ;

        JobRunr
                .configure()
                .useStorageProvider(storageProvider)
                .useDashboard()
                .useDefaultBackgroundJobServer()
                .initialize();

        BackgroundJob.<TestService>enqueue(x -> x.doWorkThatTakesLong(JobContext.Null));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> Thread.currentThread().interrupt()));

        Thread.currentThread().join();
    }
}
