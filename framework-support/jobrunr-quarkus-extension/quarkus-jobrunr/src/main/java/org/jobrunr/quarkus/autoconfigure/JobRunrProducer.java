package org.jobrunr.quarkus.autoconfigure;

import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.lookup.LookupIfProperty;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.jobrunr.dashboard.JobRunrDashboardWebServerConfiguration;
import org.jobrunr.server.carbonaware.CarbonAwareConfiguration;
import org.jobrunr.server.carbonaware.CarbonAwareJobManager;
import org.jobrunr.jobs.details.CachingJobDetailsGenerator;
import org.jobrunr.jobs.details.JobDetailsGenerator;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.jobrunr.utils.mapper.jsonb.JsonbJsonMapper;

import java.time.Duration;

import static java.util.Collections.emptyList;
import static org.jobrunr.utils.reflection.ReflectionUtils.newInstance;

@Singleton
public class JobRunrProducer {

    @Inject
    JobRunrRuntimeConfiguration jobRunrRuntimeConfiguration;

    @Produces
    @DefaultBean
    @Singleton
    public CarbonAwareJobManager carbonAwareJobManager(JsonMapper jobRunrJsonMapper) {
        CarbonAwareConfiguration carbonAwareConfiguration = CarbonAwareConfiguration.usingStandardCarbonAwareConfiguration();
        jobRunrRuntimeConfiguration.jobs().carbonAwareConfiguration().areaCode().ifPresent(carbonAwareConfiguration::andAreaCode);
        jobRunrRuntimeConfiguration.jobs().carbonAwareConfiguration().apiClientConnectTimeoutMs().ifPresent(connectTimeout -> carbonAwareConfiguration.andApiClientConnectTimeout(Duration.ofMillis(connectTimeout)));
        jobRunrRuntimeConfiguration.jobs().carbonAwareConfiguration().apiClientReadTimeoutMs().ifPresent(readTimeout -> carbonAwareConfiguration.andApiClientReadTimeout(Duration.ofMillis(readTimeout)));
        return new CarbonAwareJobManager(carbonAwareConfiguration, jobRunrJsonMapper);
    }

    @Produces
    @DefaultBean
    @Singleton
    @LookupIfProperty(name = "quarkus.jobrunr.job-scheduler.enabled", stringValue = "true")
    public JobScheduler jobScheduler(StorageProvider storageProvider) {
        if (jobRunrRuntimeConfiguration.jobScheduler().enabled()) {
            final JobDetailsGenerator jobDetailsGenerator = newInstance(jobRunrRuntimeConfiguration.jobScheduler().jobDetailsGenerator().orElse(CachingJobDetailsGenerator.class.getName()));
            return new JobScheduler(storageProvider, jobDetailsGenerator, emptyList());
        }
        return null;
    }

    @Produces
    @DefaultBean
    @Singleton
    @LookupIfProperty(name = "quarkus.jobrunr.job-scheduler.enabled", stringValue = "true")
    public JobRequestScheduler jobRequestScheduler(StorageProvider storageProvider) {
        if (jobRunrRuntimeConfiguration.jobScheduler().enabled()) {
            return new JobRequestScheduler(storageProvider, emptyList());
        }
        return null;
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
