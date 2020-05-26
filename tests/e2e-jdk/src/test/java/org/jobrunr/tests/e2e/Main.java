package org.jobrunr.tests.e2e;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.JobActivator;
import org.jobrunr.storage.SimpleStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.tests.e2e.services.TestService;
import org.jobrunr.utils.mapper.gson.GsonJsonMapper;

import java.util.Calendar;
import java.util.UUID;

import static java.util.Arrays.asList;

public class Main {
    private TestService testService;

    public Main(String[] args) throws Exception {
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
        final BackgroundJobServer backgroundJobServer = new BackgroundJobServer(storageProvider, getJobActivator());
        JobRunr
                .configure()
                .useStorageProvider(storageProvider)
                .useBackgroundJobServer(backgroundJobServer)
                .useDashboard()
                .initialize();
        if (startRunning) {
            backgroundJobServer.start();
        }

        onStartup();
        logStartWaitForeverAndAddShutdownHook();
    }

    protected StorageProvider initStorageProvider() {
        return new SimpleStorageProvider()
                .withJsonMapper(new GsonJsonMapper());
    }

    protected void onStartup() {
        BackgroundJob.enqueue(() -> testService.doWork(UUID.randomUUID()));
    }

    protected JobActivator getJobActivator() {
        testService = new TestService();
        return new JobActivator() {
            @Override
            public <T> T activateJob(Class<T> type) {
                return (T) testService;
            }
        };
    }

    private void logStartWaitForeverAndAddShutdownHook() throws InterruptedException {
        System.out.println(Calendar.getInstance().getTime() + " - Background Job server is ready ");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> Thread.currentThread().interrupt()));

        Thread.currentThread().join();
    }
}
