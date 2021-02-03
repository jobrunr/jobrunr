package org.jobrunr.dashboard;

import org.jobrunr.SevereJobRunrException;
import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.scheduling.cron.Cron;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.SevereExceptionManager;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StubDataProvider;
import org.jobrunr.stubs.TestService;
import org.jobrunr.utils.diagnostics.DiagnosticsBuilder;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Timer;
import java.util.TimerTask;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.jobrunr.jobs.JobDetailsTestBuilder.classThatDoesNotExistJobDetails;
import static org.jobrunr.jobs.JobDetailsTestBuilder.methodThatDoesNotExistJobDetails;
import static org.jobrunr.jobs.JobTestBuilder.aJob;
import static org.jobrunr.utils.diagnostics.DiagnosticsBuilder.diagnostics;

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

        BackgroundJobServer backgroundJobServer = new BackgroundJobServer(storageProvider);
        JobRunr
                .configure()
                .useStorageProvider(storageProvider)
                .useDashboardIf(dashboardIsEnabled(args), 8000)
                .useBackgroundJobServer(backgroundJobServer)
                .initialize();

        backgroundJobServer.start();
        BackgroundJob.<TestService>scheduleRecurrently("Github-75", x -> x.doWorkThatTakesLong(JobContext.Null), Cron.daily(11, 42), ZoneId.of("America/New_York"));

        SevereExceptionManager severeExceptionManager = new SevereExceptionManager(backgroundJobServer);
        new Timer().schedule(new TimerTask() {
                                 @Override
                                 public void run() {
                                     severeExceptionManager.handle(new SevereJobRunrException("A bad exception happened.", new ExceptionWithDiagnostics()));
                                     System.out.println("Saved ServerJobRunrException");
                                 }
                             },
                30000
        );

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

    private static class ExceptionWithDiagnostics implements SevereJobRunrException.DiagnosticsAware {

        @Override
        public DiagnosticsBuilder getDiagnosticsInfo() {
            return diagnostics().withTitle("Title").withLine("Text").withException(new RuntimeException());
        }
    }
}
