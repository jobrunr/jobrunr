package org.jobrunr.dashboard;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jobrunr.SevereJobRunrException;
import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.mappers.JobMapper;
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
import java.time.Duration;
import java.time.LocalDate;
import java.util.Timer;
import java.util.TimerTask;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.jobrunr.jobs.JobTestBuilder.aJob;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.jobrunr.server.carbonaware.CarbonAwareJobProcessingConfiguration.usingStandardCarbonAwareJobProcessingConfiguration;
import static org.jobrunr.utils.diagnostics.DiagnosticsBuilder.diagnostics;

/**
 * Main Class to run for FrontEndDevelopment
 */
public class FrontEndDevelopment {

    public static void main(String[] args) throws Exception {
        StorageProvider storageProvider = inMemoryStorageProvider();

        //StubDataProvider.using(storageProvider)
        //.addALotOfEnqueuedJobsThatTakeSomeTime()
        //.addALotOfEnqueuedJobsThatTakeSomeTime()
        //.addSomeRecurringJobs();

//        storageProvider.save(aJob().withJobDetails(classThatDoesNotExistJobDetails()).withState(new ScheduledState(Instant.now().plus(2, MINUTES))).build());
//        storageProvider.save(aJob().withJobDetails(methodThatDoesNotExistJobDetails()).withState(new ScheduledState(Instant.now().plus(2, MINUTES))).build());
//        storageProvider.save(aJob().withJobDetails(jobParameterThatDoesNotExistJobDetails()).withState(new ScheduledState(Instant.now().plus(1, MINUTES))).build());

        storageProvider.save(anEnqueuedJob().withName("A job with label").withLabels("Label 1", "Label 3", "Label 2").build());
        storageProvider.save(aJob().withEnqueuedState(now()).withName("A job").build());

        var carbonConfig = usingStandardCarbonAwareJobProcessingConfiguration()
                .andAreaCode("BE");

        JobRunr
                .configure()
                .useJsonMapper(new JacksonJsonMapper())
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
        BackgroundJob.<TestService>scheduleRecurrently("0 14 * * *", x -> x.doWorkThatTakesLong(40));

        BackgroundJob.<TestService>schedule(CarbonAware.at(now().plus(4, HOURS), Duration.ofHours(4)), x -> x.doWork(4));
        BackgroundJob.<TestService>schedule(CarbonAware.at(now().plus(24, HOURS), Duration.ofHours(4)), x -> x.doWork(28));
        BackgroundJob.<TestService>schedule(CarbonAware.at(now().plus(48, HOURS), Duration.ofHours(4)), x -> x.doWork(52));
        BackgroundJob.<TestService>schedule(CarbonAware.at(now().plus(3, DAYS), Duration.ofHours(4)), x -> x.doWork(72));
        BackgroundJob.<TestService>schedule(CarbonAware.between(LocalDate.now().atTime(20, 0), LocalDate.now().atTime(22, 0)), x -> x.doWork(72));
        BackgroundJob.<TestService>schedule(CarbonAware.at(now().minus(10, DAYS), Duration.ofHours(4)), x -> x.doWork(-240));

        BackgroundJob.<TestService>enqueue(x -> x.doWorkThatTakesLong(JobContext.Null));

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
