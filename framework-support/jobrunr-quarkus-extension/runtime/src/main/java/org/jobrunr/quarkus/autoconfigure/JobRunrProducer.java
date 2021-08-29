package org.jobrunr.quarkus.autoconfigure;

import io.quarkus.arc.DefaultBean;
import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.jobrunr.dashboard.JobRunrDashboardWebServerConfiguration;
import org.jobrunr.jobs.details.CachingJobDetailsGenerator;
import org.jobrunr.jobs.details.JobDetailsGenerator;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.server.JobActivator;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.jobrunr.utils.mapper.jsonb.JsonbJsonMapper;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Singleton;

import static java.util.Collections.emptyList;
import static org.jobrunr.dashboard.JobRunrDashboardWebServerConfiguration.usingStandardDashboardConfiguration;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.jobrunr.utils.reflection.ReflectionUtils.newInstance;

@Singleton
public class JobRunrProducer {

    @Inject
    JobRunrConfiguration configuration;

    @Produces
    @DefaultBean
    @Singleton
    public JobScheduler jobScheduler(StorageProvider storageProvider) {
        if (configuration.jobScheduler.enabled) {
            final JobDetailsGenerator jobDetailsGenerator = newInstance(configuration.jobScheduler.jobDetailsGenerator.orElse(CachingJobDetailsGenerator.class.getName()));
            return new JobScheduler(storageProvider, jobDetailsGenerator, emptyList());
        }
        return null;
    }

    @Produces
    @DefaultBean
    @Singleton
    public JobRequestScheduler jobRequestScheduler(StorageProvider storageProvider) {
        if (configuration.jobScheduler.enabled) {
            return new JobRequestScheduler(storageProvider, emptyList());
        }
        return null;
    }

    @Produces
    @DefaultBean
    @Singleton
    BackgroundJobServerConfiguration backgroundJobServerConfiguration() {
        if (configuration.backgroundJobServer.enabled) {
            final BackgroundJobServerConfiguration backgroundJobServerConfiguration = usingStandardBackgroundJobServerConfiguration();
            configuration.backgroundJobServer.pollIntervalInSeconds.ifPresent(backgroundJobServerConfiguration::andPollIntervalInSeconds);
            configuration.backgroundJobServer.workerCount.ifPresent(backgroundJobServerConfiguration::andWorkerCount);
            configuration.backgroundJobServer.deleteSucceededJobsAfter.ifPresent(backgroundJobServerConfiguration::andDeleteSucceededJobsAfter);
            configuration.backgroundJobServer.permanentlyDeleteDeletedJobsAfter.ifPresent(backgroundJobServerConfiguration::andPermanentlyDeleteDeletedJobsAfter);
            return backgroundJobServerConfiguration;
        }
        return null;
    }

    @Produces
    @DefaultBean
    @Singleton
    BackgroundJobServer backgroundJobServer(StorageProvider storageProvider, JsonMapper jobRunrJsonMapper, JobActivator jobActivator, BackgroundJobServerConfiguration backgroundJobServerConfiguration) {
        if (configuration.backgroundJobServer.enabled) {
            return new BackgroundJobServer(storageProvider, jobRunrJsonMapper, jobActivator, backgroundJobServerConfiguration);
        }
        return null;
    }

    @Produces
    @DefaultBean
    @Singleton
    JobRunrDashboardWebServerConfiguration dashboardWebServerConfiguration() {
        if (configuration.dashboard.enabled) {
            final JobRunrDashboardWebServerConfiguration dashboardWebServerConfiguration = usingStandardDashboardConfiguration();
            configuration.dashboard.port.ifPresent(dashboardWebServerConfiguration::andPort);
            if (configuration.dashboard.username.isPresent() && configuration.dashboard.password.isPresent()) {
                dashboardWebServerConfiguration.andBasicAuthentication(configuration.dashboard.username.get(), configuration.dashboard.password.get());
            }
            return dashboardWebServerConfiguration;
        }
        return null;
    }

    @Produces
    @DefaultBean
    @Singleton
    JobRunrDashboardWebServer dashboardWebServer(StorageProvider storageProvider, JsonMapper jobRunrJsonMapper, JobRunrDashboardWebServerConfiguration dashboardWebServerConfiguration) {
        if (configuration.dashboard.enabled) {
            return new JobRunrDashboardWebServer(storageProvider, jobRunrJsonMapper, dashboardWebServerConfiguration);
        }
        return null;
    }

    @Produces
    @DefaultBean
    @Singleton
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
    @Singleton
    public JobMapper jobMapper(JsonMapper jobRunrJsonMapper) {
        return new JobMapper(jobRunrJsonMapper);
    }


    public static class JobRunrJsonBJsonMapperProducer {
        @Produces
        @DefaultBean
        @Singleton
        public JsonMapper jobRunrJsonMapper() {
            return new JsonbJsonMapper();
        }

    }

    public static class JobRunrJacksonJsonMapperProducer {
        @Produces
        @DefaultBean
        @Singleton
        public JsonMapper jobRunrJsonMapper() {
            return new JacksonJsonMapper();
        }

    }
}
