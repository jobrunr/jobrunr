package org.jobrunr.tests.e2e;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.StorageProvider;

import java.util.Arrays;
import java.util.Calendar;

import static java.util.Arrays.asList;

public abstract class AbstractMain {

    private StorageProvider storageProvider;

    protected abstract StorageProvider initStorageProvider() throws Exception;

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
        storageProvider = initStorageProvider();
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
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Thread.currentThread().interrupt();
        }));

        Thread.currentThread().join();
    }

    protected static String getEnvOrProperty(String name) {
        if (System.getProperty(name) != null) return System.getProperty(name);
        if (System.getenv(name) != null) return System.getenv(name);
        return null;
    }
}
