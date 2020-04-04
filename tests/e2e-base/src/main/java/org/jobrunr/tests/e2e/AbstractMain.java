package org.jobrunr.tests.e2e;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.common.SqlJobStorageProviderFactory;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Calendar;

import static java.util.Arrays.asList;

public abstract class AbstractMain {

    protected abstract DataSource createDataSource(String jdbcUrl, String userName, String password) throws SQLException;

    protected StorageProvider initStorageProvider() throws Exception {
        if (getEnvOrProperty("JOBRUNR_JDBC_URL") == null) {
            throw new IllegalStateException("Cannot start BackgroundJobServer: environment variable JOBRUNR_JDBC_URL is not set");
        }

        DataSource dataSource = createDataSource(
                getEnvOrProperty("JOBRUNR_JDBC_URL"),
                getEnvOrProperty("JOBRUNR_JDBC_USERNAME"),
                getEnvOrProperty("JOBRUNR_JDBC_PASSWORD"));

        return SqlJobStorageProviderFactory.using(dataSource);
    }

    public AbstractMain(String[] args) throws Exception {
        if (args.length < 1) {
            startBackgroundJobServerInRunningState();
        } else if (asList(args).contains("--pause")) {
            startBackgroundJobServerInPausedState();
        } else {
            System.out.println("Did not start server");
        }
    }

    protected void startBackgroundJobServerInRunningState() throws Exception {
        startBackgroundJobServer(true);
    }

    protected void startBackgroundJobServerInPausedState() throws Exception {
        startBackgroundJobServer(false);
    }

    private void startBackgroundJobServer(boolean startRunning) throws Exception {
        StorageProvider storageProvider = initStorageProvider();
        final BackgroundJobServer backgroundJobServer = new BackgroundJobServer(storageProvider);
        JobRunr
                .configure()
                .useJobStorageProvider(storageProvider)
                .useBackgroundJobServer(backgroundJobServer)
                .useDashboard()
                .initialize();
        if(startRunning) {
            backgroundJobServer.start();
        }

        logStartWaitForeverAndAddShutdownHook();
    }

    private void logStartWaitForeverAndAddShutdownHook() throws InterruptedException {
        System.out.println(Calendar.getInstance().getTime() + " - Background Job server is ready ");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> Thread.currentThread().interrupt()));

        Thread.currentThread().join();
    }

    protected static String getEnvOrProperty(String name) {
        if (System.getProperty(name) != null) return System.getProperty(name);
        if (System.getenv(name) != null) return System.getenv(name);
        return null;
    }
}
