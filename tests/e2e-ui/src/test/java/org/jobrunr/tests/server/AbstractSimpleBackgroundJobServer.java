package org.jobrunr.tests.server;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.configuration.JobRunrConfiguration;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.gson.GsonJsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.jobrunr.utils.mapper.jsonb.JsonbJsonMapper;

import java.util.Calendar;

import static java.util.Arrays.asList;

public abstract class AbstractSimpleBackgroundJobServer {

    private JsonMapper jsonMapper;
    private boolean paused;

    public AbstractSimpleBackgroundJobServer withGsonMapper() {
        this.jsonMapper = new GsonJsonMapper();
        return this;
    }

    public AbstractSimpleBackgroundJobServer WithJacksonMapper() {
        this.jsonMapper = new JacksonJsonMapper();
        return this;
    }

    public AbstractSimpleBackgroundJobServer withJsonBMapper() {
        this.jsonMapper = new JsonbJsonMapper();
        return this;
    }
    
    public AbstractSimpleBackgroundJobServer withPaused() {
        this.paused = true;
        return this;
    }

    public void stop() {
        JobRunr.destroy();
    }

    public void start() {
        try {
            StorageProvider storageProvider = initStorageProvider();

            JobRunrConfiguration jobRunrConfiguration = JobRunr
                    .configure()
                    .useJsonMapper(jsonMapper)
                    .useStorageProvider(storageProvider);
            loadDefaultData(storageProvider);

            jobRunrConfiguration
                    .useBackgroundJobServerIf(!paused)
                    .useDashboard()
                    .initialize();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
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
    }

    protected void loadDefaultData(StorageProvider storageProvider) {
        // hook that can be implemented by subclasses
    }


}
