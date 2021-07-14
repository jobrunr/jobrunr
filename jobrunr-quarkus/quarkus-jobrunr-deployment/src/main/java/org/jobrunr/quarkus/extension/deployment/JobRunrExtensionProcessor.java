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
import java.util.function.BooleanSupplier;

class JobRunrExtensionProcessor {

    private static final String FEATURE = "jobrunr-extension";

    JobRunrConfiguration configuration;

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }


    @Produces
    @DefaultBean
    //TODO: make it return correct storageprovider based on Extensions (agroal, mongo, redis, elasticsearch)?
    public StorageProvider storageProvider() {
        return new InMemoryStorageProvider();
    }

    @Produces
    @DefaultBean
    @BuildStep(onlyIf = IsJobSchedulerEnabled.class)
    public JobScheduler jobScheduler(StorageProvider storageProvider) {
        return new JobScheduler(storageProvider);
    }

    // TODO: consume JobRunrConfiguration for custom settings
    @Produces
    @DefaultBean
    @BuildStep(onlyIf = IsBackgroundJobServerEnabled.class)
    BackgroundJobServer backgroundJobServer(StorageProvider storageProvider, JsonMapper jsonMapper, JobActivator jobActivator) {
        return new BackgroundJobServer(storageProvider, jsonMapper, jobActivator);
    }

    // TODO: consume JobRunrConfiguration for custom settings
    @Produces
    @DefaultBean
    @BuildStep(onlyIf = IsDashboardWebServerEnabled.class)
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
    //TODO: make it return correct JsonMapper based on what is available (JSONB, Jackson)?
    public JsonMapper jsonMapper() {
        return new JsonbJsonMapper();
    }

    static class IsBackgroundJobServerEnabled implements BooleanSupplier {
        JobRunrConfiguration jobRunrConfiguration;

        public boolean getAsBoolean() {
            return jobRunrConfiguration.backgroundJobServer.enabled;
        }
    }

    static class IsJobSchedulerEnabled implements BooleanSupplier {
        JobRunrConfiguration jobRunrConfiguration;

        public boolean getAsBoolean() {
            return jobRunrConfiguration.jobScheduler.enabled;
        }
    }

    static class IsDashboardWebServerEnabled implements BooleanSupplier {
        JobRunrConfiguration jobRunrConfiguration;

        public boolean getAsBoolean() {
            return jobRunrConfiguration.dashboard.enabled;
        }
    }
}
