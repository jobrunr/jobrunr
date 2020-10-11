package org.jobrunr.dashboard;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StubDataProvider;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;

import java.time.Instant;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.jobrunr.jobs.JobDetailsTestBuilder.classThatDoesNotExistJobDetails;
import static org.jobrunr.jobs.JobDetailsTestBuilder.methodThatDoesNotExistJobDetails;
import static org.jobrunr.jobs.JobTestBuilder.aJob;

/**
 * Main Class to run for FrontEndDevelopment
 */
public class FrontEndDevelopment {

    public static void main(String[] args) throws InterruptedException {
        StorageProvider storageProvider = new InMemoryStorageProvider();
        storageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));

        StubDataProvider.using(storageProvider)
                //.addALotOfEnqueuedJobsThatTakeSomeTime()
                .addSomeRecurringJobs();

        storageProvider.save(aJob().withJobDetails(classThatDoesNotExistJobDetails()).withState(new ScheduledState(Instant.now().plus(1, DAYS))).build());
        storageProvider.save(aJob().withJobDetails(methodThatDoesNotExistJobDetails()).withState(new ScheduledState(Instant.now().plus(1, DAYS))).build());

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
