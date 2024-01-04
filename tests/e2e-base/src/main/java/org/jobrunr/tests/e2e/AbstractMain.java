package org.jobrunr.tests.e2e;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.configuration.JobRunrConfiguration;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.gson.GsonJsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.jobrunr.utils.mapper.jsonb.JsonbJsonMapper;

import java.util.Calendar;

import static java.util.Arrays.asList;

public abstract class AbstractMain {

    public AbstractMain(String[] args) throws Exception {
        if (args.length < 1) {
            startBackgroundJobServerInRunningState(getJsonMapper(args));
        } else if (asList(args).contains("--pause")) {
            startBackgroundJobServerInPausedState(getJsonMapper(args));
        } else {
            System.out.println("Did not start server");
        }
    }

    private JsonMapper getJsonMapper(String[] args) {
        if (args != null && args.length > 0) {
            if (asList(args).contains("--jackson")) {
                return new JacksonJsonMapper();
            } else if (asList(args).contains("--gson")) {
                return new GsonJsonMapper();
            } else if (asList(args).contains("--jsonb")) {
                return new JsonbJsonMapper();
            }
        }
        return new JacksonJsonMapper();
    }

    protected abstract StorageProvider initStorageProvider() throws Exception;

    protected void startBackgroundJobServerInRunningState(JsonMapper jsonMapper) throws Exception {
        startBackgroundJobServer(jsonMapper, true);
    }

    protected void startBackgroundJobServerInPausedState(JsonMapper jsonMapper) throws Exception {
        startBackgroundJobServer(jsonMapper, false);
    }

    private void startBackgroundJobServer(JsonMapper jsonMapper, boolean startRunning) throws Exception {
        StorageProvider storageProvider = initStorageProvider();

        JobRunrConfiguration jobRunrConfiguration = JobRunr
                .configure()
                .useJsonMapper(jsonMapper)
                .useStorageProvider(storageProvider);
        loadDefaultData(storageProvider);

        jobRunrConfiguration
                .useBackgroundJobServerIf(startRunning)
                .useDashboard()
                .initialize();

        logStartWaitForeverAndAddShutdownHook();
    }

    protected void loadDefaultData(StorageProvider storageProvider) {
        // hook that can be implemented by subclasses
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
