package org.jobrunr.dashboard;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.scheduling.cron.Cron;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StubDataProvider;
import org.jobrunr.stubs.TestService;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;

import java.time.Instant;
import java.time.ZoneId;

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
                //.addALotOfEnqueuedJobsThatTakeSomeTime()
                .addSomeRecurringJobs();

        storageProvider.save(aJob().withJobDetails(classThatDoesNotExistJobDetails()).withState(new ScheduledState(Instant.now().plus(1, DAYS))).build());
        storageProvider.save(aJob().withJobDetails(methodThatDoesNotExistJobDetails()).withState(new ScheduledState(Instant.now().plus(1, DAYS))).build());

        JobRunr
                .configure()
                .useStorageProvider(storageProvider)
                .useDashboardIf(dashboardIsEnabled(args), 8000)
                .useDefaultBackgroundJobServer()
                .initialize();

        BackgroundJob.<TestService>scheduleRecurrently("Github-75", x -> x.doWorkThatTakesLong(JobContext.Null), Cron.daily(11, 42), ZoneId.of("America/New_York"));


        Runtime.getRuntime().addShutdownHook(new Thread(() -> Thread.currentThread().interrupt()));

        Thread.currentThread().join();
    }

    private static boolean dashboardIsEnabled(String[] args) {
        return !argsContains(args, "dashboard=false");
    }

    private static boolean argsContains(String[] args, String argToSearch) {
        if (args.length == 0) return false;
        for (String arg : args) {
            if (argToSearch.equalsIgnoreCase(arg)) return true;
        }
        return false;
    }
}
