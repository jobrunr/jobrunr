package org.jobrunr.dashboard;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jobrunr.SevereJobRunrException;
import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.scheduling.cron.Cron;
import org.jobrunr.server.dashboard.CpuAllocationIrregularityNotification;
import org.jobrunr.server.dashboard.DashboardNotificationManager;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StubDataProvider;
import org.jobrunr.stubs.TestService;
import org.jobrunr.utils.diagnostics.DiagnosticsBuilder;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.jobrunr.jobs.JobDetailsTestBuilder.*;
import static org.jobrunr.jobs.JobTestBuilder.aJob;
import static org.jobrunr.scheduling.RecurringJobBuilder.aRecurringJob;
import static org.jobrunr.utils.diagnostics.DiagnosticsBuilder.diagnostics;

/**
 * Main Class to run for FrontEndDevelopment
 */
public class FrontEndDevelopment {

    public static void main(String[] args) throws InterruptedException {
        TestService testService = new TestService();
        StorageProvider storageProvider = new InMemoryStorageProvider();
        //final StorageProvider storageProvider = SqlStorageProviderFactory.using(getMariaDBDataSource());
        storageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));

        StubDataProvider.using(storageProvider)
                //.addALotOfEnqueuedJobsThatTakeSomeTime()
                //.addALotOfEnqueuedJobsThatTakeSomeTime()
                .addSomeRecurringJobs();

        int i = 0;
        Set<String> tooManyLabels = Set.of("Label" + (++i), "Label" + (++i), "Label" + (++i), "Label" + (++i), "Label" + (++i), "Label" + (++i), "Label" + (++i), "Label" + (++i), "Label" + (++i), "Label" + (++i), "Label" + (++i), "Label" + (++i), "Label" + (++i), "Label" + (++i), "Label" + (++i), "Label" + (++i), "Label" + (++i), "Label" + (++i), "Label" + (++i));
        storageProvider.save(aJob().withJobDetails(classThatDoesNotExistJobDetails()).withLabels(tooManyLabels).withState(new ScheduledState(Instant.now().plus(2, MINUTES))).build());
        storageProvider.save(aJob().withJobDetails(methodThatDoesNotExistJobDetails()).withLabels(Set.of("test")).withState(new ScheduledState(Instant.now().plus(2, MINUTES))).build());
        storageProvider.save(aJob().withJobDetails(jobParameterThatDoesNotExistJobDetails()).withLabels(Set.of("Failed Job", "Missing job parameter")).withState(new ScheduledState(Instant.now().plus(1, MINUTES))).build());

        JobRunr
                .configure()
                .useStorageProvider(storageProvider)
                .useDashboardIf(dashboardIsEnabled(args), 8000)
                .useBackgroundJobServer()
                .initialize();

        BackgroundJob.scheduleRecurrently(
                aRecurringJob()
                        .withId("Github-75")
                        .withLabels("Triggered by someone", "Long", "provided Id")
                        .withCron(Cron.daily(18, 4))
                        .withDetails(() -> testService.doWorkThatTakesLong(JobContext.Null)));

        BackgroundJob.scheduleRecurrently(
                aRecurringJob()
                        .withLabels(Set.of("Recurring", "Long"))
                        .withDuration(Duration.ofMinutes(1))
                        .withDetails(() -> testService.doWorkThatTakesLong(JobContext.Null)));

        DashboardNotificationManager dashboardNotificationManager = new DashboardNotificationManager(JobRunr.getBackgroundJobServer().getId(), storageProvider);
        new Timer().schedule(new TimerTask() {
                                 @Override
                                 public void run() {
                                     dashboardNotificationManager.handle(new SevereJobRunrException("A bad exception happened.", new ExceptionWithDiagnostics()));
                                     dashboardNotificationManager.notify(new CpuAllocationIrregularityNotification(20));
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

    protected static DataSource getMariaDBDataSource() {
        HikariConfig config = new HikariConfig();
        config.setMaximumPoolSize(84);
        config.setJdbcUrl("jdbc:mariadb://localhost:3306/mysql?rewriteBatchedStatements=true&useBulkStmts=false");
        config.setUsername("root");
        config.setPassword("mysql");
        return new HikariDataSource(config);
    }
}
