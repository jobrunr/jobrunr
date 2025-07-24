package org.jobrunr.tests.server;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.configuration.JobRunrConfiguration;
import org.jobrunr.kotlin.utils.mapper.KotlinxSerializationJsonMapper;
import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.server.carbonaware.CarbonAwareJobProcessingConfiguration;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.gson.GsonJsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.jobrunr.utils.mapper.jsonb.JsonbJsonMapper;

import java.util.UUID;

import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;

public abstract class AbstractSimpleBackgroundJobServer {

    protected JsonMapper jsonMapper;
    protected boolean paused;
    protected CarbonAwareJobProcessingConfiguration carbonAwareConfig;
    protected UUID id;
    protected StorageProvider storageProvider;

    public AbstractSimpleBackgroundJobServer withId(UUID id) {
        this.id = id;
        return this;
    }

    public AbstractSimpleBackgroundJobServer withGsonMapper() {
        this.jsonMapper = new GsonJsonMapper();
        return this;
    }

    public AbstractSimpleBackgroundJobServer withKotlinSerializationMapper() {
        this.jsonMapper = new KotlinxSerializationJsonMapper();
        return this;
    }

    public AbstractSimpleBackgroundJobServer withJacksonMapper() {
        this.jsonMapper = new JacksonJsonMapper();
        return this;
    }

    public AbstractSimpleBackgroundJobServer withCarbonAwareJobProcessing(CarbonAwareJobProcessingConfiguration config) {
        this.carbonAwareConfig = config;
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

    public AbstractSimpleBackgroundJobServer withStorageProvider(StorageProvider storageProvider) {
        this.storageProvider = storageProvider;
        return this;
    }

    public void stop() {
        JobRunr.destroy();
    }

    public void start() {
        try {
            if (storageProvider == null) {
                storageProvider = initStorageProvider();
            }

            JobRunrConfiguration jobRunrConfiguration = JobRunr
                    .configure()
                    .useJsonMapper(jsonMapper)
                    .useStorageProvider(storageProvider);
            loadDefaultData(storageProvider);

            BackgroundJobServerConfiguration bgServerConfiguration = usingStandardBackgroundJobServerConfiguration();
            if (id != null) {
                bgServerConfiguration.andId(id);
            }
            if (carbonAwareConfig != null) {
                bgServerConfiguration.andCarbonAwareJobProcessingConfiguration(carbonAwareConfig);
            }

            jobRunrConfiguration.useBackgroundJobServer(bgServerConfiguration, !paused);

            jobRunrConfiguration
                    .useDashboard()
                    .initialize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract StorageProvider initStorageProvider() throws Exception;

    protected void loadDefaultData(StorageProvider storageProvider) {
        // hook that can be implemented by subclasses
    }


}
