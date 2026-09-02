package org.jobrunr.dashboard;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jobrunr.SevereJobRunrException;
import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.FailedState;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.scheduling.carbonaware.CarbonAware;
import org.jobrunr.server.dashboard.CpuAllocationIrregularityNotification;
import org.jobrunr.server.dashboard.DashboardNotificationManager;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory;
import org.jobrunr.stubs.TestService;
import org.jobrunr.utils.diagnostics.DiagnosticsBuilder;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static java.time.Instant.now;
import static org.jobrunr.jobs.JobTestBuilder.aJob;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.jobrunr.server.carbonaware.CarbonAwareJobProcessingConfiguration.usingStandardCarbonAwareJobProcessingConfiguration;
import static org.jobrunr.utils.diagnostics.DiagnosticsBuilder.diagnostics;

/**
 * Main Class to run for FrontEndDevelopment
 */
@SuppressWarnings("unused")
public class FrontEndDevelopment {

    public static void main(String[] args) throws Exception {
        StorageProvider storageProvider = inMemoryStorageProvider();
        JacksonJsonMapper jsonMapper = new JacksonJsonMapper();

        //StubDataProvider.using(storageProvider)
        //.addALotOfEnqueuedJobsThatTakeSomeTime()
        //.addALotOfEnqueuedJobsThatTakeSomeTime()
        //.addSomeRecurringJobs();

//        storageProvider.save(aJob().withJobDetails(classThatDoesNotExistJobDetails()).withState(new ScheduledState(Instant.now().plus(2, MINUTES))).build());
//        storageProvider.save(aJob().withJobDetails(methodThatDoesNotExistJobDetails()).withState(new ScheduledState(Instant.now().plus(2, MINUTES))).build());
//        storageProvider.save(aJob().withJobDetails(jobParameterThatDoesNotExistJobDetails()).withState(new ScheduledState(Instant.now().plus(1, MINUTES))).build());

        storageProvider.save(anEnqueuedJob().withName("A job with label").withLabels("Label 1", "Label 3", "Label 2").build());
        storageProvider.save(aJob().withEnqueuedState(now()).withName("A job").build());

        Instant now = Instant.now();
        storageProvider.save(aJob().withName("A job that succeeded after some retries")
                .withId(UUID.fromString("01a061e8-0ce5-717b-a4e7-8b9d91535da0"))
                .withAmountOfRetries(5)
                .withEnqueuedState(now.minus(8, ChronoUnit.HOURS))
                .withProcessingState(now.minus(8, ChronoUnit.HOURS))
                .withState(new FailedState("Failed", null, null, null, null, null, false, now.minus(8 * 60 - 10, ChronoUnit.MINUTES)))
                .withState(new ScheduledState(now.minus(4 * 60 - 20, ChronoUnit.MINUTES), "Retry attempt 1 of 5", now.minus(8 * 60 - 10, ChronoUnit.MINUTES)))
                .withEnqueuedState(now.minus(4 * 60 - 20, ChronoUnit.MINUTES))
                .withProcessingState(now.minus(4 * 60 - 21, ChronoUnit.MINUTES))
                .withState(new FailedState("Failed", null, null, null, null, null, false, now.minus(2 * 60, ChronoUnit.MINUTES)))
                .withState(new ScheduledState(now.minus(2 * 60 - 40, ChronoUnit.MINUTES), "Retry attempt 1 of 5", now.minus(2 * 60, ChronoUnit.MINUTES)))
                .withEnqueuedState(now.minus(2 * 60 - 40, ChronoUnit.MINUTES))
                .withProcessingState(now.minus(2 * 60 - 44, ChronoUnit.MINUTES))
                .withSucceededState()
                .withMetadata(Map.ofEntries(
                        Map.entry("jr_step_first-step__2", false),
                        Map.entry("jr_step_start_first-step__2", now.minus(8 * 60 - 5, ChronoUnit.MINUTES)),
                        Map.entry("jr_step_end_first-step__2", now.minus(8 * 60 - 10, ChronoUnit.MINUTES)),

                        Map.entry("jr_step_first-step__6", true),
                        Map.entry("jr_step_start_first-step__6", now.minus(4 * 60 - 21, ChronoUnit.MINUTES)),
                        Map.entry("jr_step_end_first-step__6", now.minus(4 * 60 - 23, ChronoUnit.MINUTES)),
                        Map.entry("jr_step_result_first-step__6", "Some amazing result"),
                        Map.entry("jr_step_result_class_first-step__6", "java.lang.String"),
                        Map.entry("jr_step_second-step__6", true),
                        Map.entry("jr_step_start_second-step__6", now.minus(4 * 60 - 23, ChronoUnit.MINUTES)),
                        Map.entry("jr_step_end_second-step__6", now.minus(3 * 60, ChronoUnit.MINUTES)),
                        Map.entry("jr_step_result_second-step__6", "Some second amazing result"),
                        Map.entry("jr_step_result_class_second-step__6", "java.lang.String"),
                        Map.entry("jr_step_third-step__6", false),
                        Map.entry("jr_step_start_third-step__6", now.minus(3 * 60, ChronoUnit.MINUTES)),
                        Map.entry("jr_step_end_third-step__6", now.minus(2 * 60, ChronoUnit.MINUTES)),

                        Map.entry("jr_step_third-step__10", true),
                        Map.entry("jr_step_start_third-step__10", now.minus(2 * 60 - 44, ChronoUnit.MINUTES)),
                        Map.entry("jr_step_end_third-step__10", now.minus((2 * 60 - 44) * 60 - 10, ChronoUnit.SECONDS)),
                        Map.entry("jr_step_result_third-step__10", "Some third amazing result"),
                        Map.entry("jr_step_result_class_third-step__10", "java.lang.String"),
                        Map.entry("jr_step_fourth-step__10", true),
                        Map.entry("jr_step_start_fourth-step__10", now.minus(2 * 60 - 50, ChronoUnit.MINUTES)),
                        Map.entry("jr_step_end_fourth-step__10", now.minus(60, ChronoUnit.MINUTES)),
                        Map.entry("jr_step_result_fourth-step__10", "Some fourth amazing result"),
                        Map.entry("jr_step_result_class_fourth-step__10", "java.lang.String"),
                        Map.entry("jr_step_fifth-step__10", true),
                        Map.entry("jr_step_start_fifth-step__10", now.minus(60, ChronoUnit.MINUTES)),
                        Map.entry("jr_step_end_fifth-step__10", now.minus(10, ChronoUnit.SECONDS)),
                        Map.entry("jr_step_result_fifth-step__10", "Some fifth amazing result"),
                        Map.entry("jr_step_result_class_fifth-step__10", "java.lang.String")
                ))
                .build());

        storageProvider.save(aJob().withName("A job that was scheduled early")
                .withAmountOfRetries(5)
                .withState(new ScheduledState(now.minus(1, ChronoUnit.DAYS), "Scheduled ahead of time", now.minus(8, ChronoUnit.DAYS)))
                .withEnqueuedState(now.minus(1, ChronoUnit.DAYS))
                .withProcessingState(now.minus(1, ChronoUnit.MINUTES))
                .withSucceededState()
                .build());

        storageProvider.save(aJob().withName("A job that backed off exponentially")
                .withAmountOfRetries(5)
                .withEnqueuedState(now.minus(2, ChronoUnit.DAYS))
                .withProcessingState(now.minus(2 * 24 * 60 * 60 - 1, ChronoUnit.SECONDS))
                .withState(new FailedState("Failed", null, null, null, null, null, false, now.minus(2 * 24 * 60 - 5, ChronoUnit.MINUTES)))
                .withState(new ScheduledState(now.minus(24 * 60 - 5, ChronoUnit.MINUTES), "Scheduled ahead of time", now.minus(2 * 24 * 60 - 5, ChronoUnit.MINUTES)))
                .withProcessingState(now.minus(24 * 60 - 5, ChronoUnit.MINUTES))
                .withSucceededState(now.minus((24 * 60 - 5) * 60 - 10, ChronoUnit.SECONDS))
                .build());

        var carbonConfig = usingStandardCarbonAwareJobProcessingConfiguration()
                .andAreaCode("BE");

        JobRunr
                .configure()
                .useJsonMapper(jsonMapper)
                .useStorageProvider(storageProvider)
                .useDashboardIf(dashboardIsEnabled(args), 8000)
                .useBackgroundJobServer(usingStandardBackgroundJobServerConfiguration()
                        .andCarbonAwareJobProcessingConfiguration(carbonConfig))
                .initialize();

        BackgroundJob.<TestService>scheduleRecurrently("carbon-aware-rj-1", CarbonAware.dailyBetween(12, 18), x -> x.doWorkWithJobAnnotationAndLabels(1, "carbon-aware"));

//        BackgroundJob.<TestService>scheduleRecurrently("carbon-aware-rj-2", CarbonAware.dailyBefore(7), x -> x.doWorkWithJobAnnotationAndLabels(1, "carbon-aware"));
//        BackgroundJob.<TestService>scheduleRecurrently("carbon-aware-rj-3", CarbonAware.using(Cron.daily(4), Duration.ofHours(2), Duration.ofHours(1)), x -> x.doWorkWithJobAnnotationAndLabels(1, "carbon-aware"));
//        BackgroundJob.<TestService>scheduleRecurrently("carbon-aware-rj-4", CarbonAware.using(Cron.daily(4), Duration.ofHours(4), Duration.ZERO), x -> x.doWorkWithJobAnnotationAndLabels(1, "carbon-aware"));
//        BackgroundJob.<TestService>scheduleRecurrently("carbon-aware-rj-5", "0 0 1 * * [P2DT6H/P10DT12H4M29.45S]", x -> x.doWorkWithJobAnnotationAndLabels(1, "carbon-aware"));
//        BackgroundJob.<TestService>scheduleRecurrently("normal-rj", Cron.daily(), x -> x.doWorkWithJobAnnotationAndLabels(1, "eager"));

        //BackgroundJob.<TestService>scheduleRecurrently(Duration.ofMinutes(1), x -> x.doWorkThatTakesLong(JobContext.Null));
//        BackgroundJob.<TestService>scheduleRecurrently(Cron.every30seconds(), x -> x.doWorkThatTakesLong(15));

//        BackgroundJob.<TestService>schedule(CarbonAware.at(now().plus(4, HOURS), Duration.ofHours(4)), x -> x.doWork(4));
//        BackgroundJob.<TestService>schedule(CarbonAware.at(now().plus(24, HOURS), Duration.ofHours(4)), x -> x.doWork(28));
//        BackgroundJob.<TestService>schedule(CarbonAware.at(now().plus(48, HOURS), Duration.ofHours(4)), x -> x.doWork(52));
//        BackgroundJob.<TestService>schedule(CarbonAware.at(now().plus(3, DAYS), Duration.ofHours(4)), x -> x.doWork(72));
//        BackgroundJob.<TestService>schedule(CarbonAware.between(nowUsingSystemDefault().atTime(20, 0), nowUsingSystemDefault().atTime(22, 0)), x -> x.doWork(72));
//        BackgroundJob.<TestService>schedule(CarbonAware.at(now().minus(10, DAYS), Duration.ofHours(4)), x -> x.doWork(-240));

        BackgroundJob.<TestService>enqueue(x -> x.doWorkThatRunsStepsOnce(JobContext.Null));

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

    private static class ExceptionWithDiagnostics extends Exception implements SevereJobRunrException.DiagnosticsAware {

        @Override
        public DiagnosticsBuilder getDiagnosticsInfo() {
            return diagnostics().withTitle("Title").withLine("Text").withException(new RuntimeException());
        }
    }

    private static StorageProvider inMemoryStorageProvider() throws SQLException {
        StorageProvider storageProvider = new InMemoryStorageProvider();
        storageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
        return storageProvider;
    }

    private static StorageProvider db2StorageProvider() throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:db2://127.0.0.1:53759/test");
        config.setUsername("db2inst1");
        config.setPassword("foobar1234");
        return toStorageProvider(new HikariDataSource(config));
    }

    private static StorageProvider h2StorageProvider() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:/tmp/test-frontend");
        config.setUsername("sa");
        config.setPassword("sa");
        return toStorageProvider(new HikariDataSource(config));
    }

    private static StorageProvider mariaDBStorageProvider() throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mariadb://localhost:3306/mysql?rewriteBatchedStatements=true&useBulkStmts=false");
        config.setUsername("root");
        config.setPassword("mysql");
        return toStorageProvider(new HikariDataSource(config));
    }

    private static StorageProvider mysqlStorageProvider() throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("com.mysql.jdbc.Driver");
        config.setJdbcUrl("jdbc:mysql://127.0.0.1:50516/test?rewriteBatchedStatements=true&useSSL=false");
        config.setUsername("test");
        config.setPassword("test");
        return toStorageProvider(new HikariDataSource(config));
    }

    private static StorageProvider oracleStorageProvider() throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:oracle:thin:@127.0.0.1:54076/xepdb1");
        config.setUsername("test");
        config.setPassword("test");
        return toStorageProvider(new HikariDataSource(config));
    }

    private static StorageProvider postgresStorageProvider() throws SQLException {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL("jdbc:postgresql://127.0.0.1:5432/postgres");
        dataSource.setUser("postgres");
        dataSource.setPassword("postgres");
        dataSource.setProperty("socketTimeout", "10");
        return toStorageProvider(dataSource);
    }

    private static StorageProvider sqliteStorageProvider() throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:/tmp/jobrunr-frontend.db");
        return toStorageProvider(new HikariDataSource(config));
    }

    private static StorageProvider sqlServerStorageProvider() throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlserver://localhost:1433;databaseName=tempdb;encrypt=true;trustServerCertificate=true;");
        config.setUsername("sa");
        config.setPassword("yourStrong(!)Password");
        return toStorageProvider(new HikariDataSource(config));
    }

    private static StorageProvider toStorageProvider(DataSource dataSource) {
        StorageProvider storageProvider = SqlStorageProviderFactory.using(dataSource);
        storageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
        return storageProvider;
    }
}
