package org.jobrunr.quarkus.extension.deployment;

import io.quarkus.arc.DefaultBean;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.jobrunr.quarkus.JobRunrConfiguration;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.JobActivator;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jsonb.JsonbJsonMapper;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.CDI;

class JobRunrExtensionProcessor {

    private static final String FEATURE = "jobrunr-extension";

    JobRunrConfiguration configuration;

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @Produces
    @DefaultBean
    public StorageProvider storageProvider() {
        return new InMemoryStorageProvider();
    }

    @Produces
    @DefaultBean
    public JobScheduler jobScheduler(StorageProvider storageProvider) {
        return new JobScheduler(storageProvider);
    }

    @Produces
    @DefaultBean
    BackgroundJobServer backgroundJobServer(StorageProvider storageProvider, JsonMapper jsonMapper, JobActivator jobActivator) {
        return new BackgroundJobServer(storageProvider, jsonMapper, jobActivator);
    }

    @Produces
    @DefaultBean
    JobRunrDashboardWebServer dashboardWebServer(StorageProvider storageProvider, JsonMapper jsonMapper) {
        return new JobRunrDashboardWebServer(storageProvider, jsonMapper);
    }

    @Produces
    @DefaultBean
    public JobActivator jobActivator() {
        return new JobActivator() {
            @Override
            public <T> T activateJob(Class<T> aClass) {
                return CDI.current().select(aClass).get();
            }
        };
    }

    @Produces
    @DefaultBean
    public JsonMapper jsonMapper() {
        return new JsonbJsonMapper();
    }
}
