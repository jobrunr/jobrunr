package org.jobrunr.quarkus.autoconfigure;

import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.jobrunr.dashboard.JobRunrDashboardWebServerConfiguration;
import org.jobrunr.jobs.details.CachingJobDetailsGenerator;
import org.jobrunr.jobs.details.JobDetailsGenerator;
import org.jobrunr.jobs.filters.RetryFilter;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.server.JobActivator;
import org.jobrunr.jobs.carbonaware.CarbonAwareConfiguration;
import org.jobrunr.jobs.carbonaware.CarbonAwareJobManager;
import org.jobrunr.server.configuration.BackgroundJobServerThreadType;
import org.jobrunr.server.configuration.BackgroundJobServerWorkerPolicy;
import org.jobrunr.server.configuration.DefaultBackgroundJobServerWorkerPolicy;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.jobrunr.utils.mapper.jsonb.JsonbJsonMapper;

import java.time.Duration;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.jobrunr.dashboard.JobRunrDashboardWebServerConfiguration.usingStandardDashboardConfiguration;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.jobrunr.utils.reflection.ReflectionUtils.newInstance;

@Singleton
public class JobRunrProducer {

    @Inject
    JobRunrBuildTimeConfiguration jobRunrBuildTimeConfiguration;

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
    public JobScheduler jobScheduler(StorageProvider storageProvider, CarbonAwareJobManager carbonAwareJobManager) {
        if (jobRunrBuildTimeConfiguration.jobScheduler().enabled()) {
            final JobDetailsGenerator jobDetailsGenerator = newInstance(jobRunrRuntimeConfiguration.jobScheduler().jobDetailsGenerator().orElse(CachingJobDetailsGenerator.class.getName()));
            return new JobScheduler(storageProvider, carbonAwareJobManager, jobDetailsGenerator, emptyList());
        }
        return null;
    }

    @Produces
    @DefaultBean
    @Singleton
    public JobRequestScheduler jobRequestScheduler(StorageProvider storageProvider, CarbonAwareJobManager carbonAwareJobManager) {
        if (jobRunrBuildTimeConfiguration.jobScheduler().enabled()) {
            return new JobRequestScheduler(storageProvider, carbonAwareJobManager, emptyList());
        }
        return null;
    }

    @Produces
    @DefaultBean
    @Singleton
    BackgroundJobServerWorkerPolicy backgroundJobServerWorkerPolicy() {
        if (jobRunrBuildTimeConfiguration.backgroundJobServer().enabled()) {
            BackgroundJobServerThreadType threadType = jobRunrRuntimeConfiguration.backgroundJobServer().threadType().orElse(BackgroundJobServerThreadType.getDefaultThreadType());
            int workerCount = jobRunrRuntimeConfiguration.backgroundJobServer().workerCount().orElse(threadType.getDefaultWorkerCount());
            return new DefaultBackgroundJobServerWorkerPolicy(workerCount, threadType);
        }
        return null;
    }

    @Produces
    @DefaultBean
    @Singleton
    BackgroundJobServerConfiguration backgroundJobServerConfiguration(BackgroundJobServerWorkerPolicy backgroundJobServerWorkerPolicy) {
        if (jobRunrBuildTimeConfiguration.backgroundJobServer().enabled()) {
            final BackgroundJobServerConfiguration backgroundJobServerConfiguration = usingStandardBackgroundJobServerConfiguration();

            backgroundJobServerConfiguration.andBackgroundJobServerWorkerPolicy(backgroundJobServerWorkerPolicy);

            jobRunrRuntimeConfiguration.backgroundJobServer().name().ifPresent(backgroundJobServerConfiguration::andName);
            jobRunrRuntimeConfiguration.backgroundJobServer().pollIntervalInSeconds().ifPresent(backgroundJobServerConfiguration::andPollIntervalInSeconds);
            jobRunrRuntimeConfiguration.backgroundJobServer().deleteSucceededJobsAfter().ifPresent(backgroundJobServerConfiguration::andDeleteSucceededJobsAfter);
            jobRunrRuntimeConfiguration.backgroundJobServer().permanentlyDeleteDeletedJobsAfter().ifPresent(backgroundJobServerConfiguration::andPermanentlyDeleteDeletedJobsAfter);
            jobRunrRuntimeConfiguration.backgroundJobServer().carbonAwatingJobsRequestSize().ifPresent(backgroundJobServerConfiguration::andCarbonAwaitingJobsRequestSize);
            jobRunrRuntimeConfiguration.backgroundJobServer().scheduledJobsRequestSize().ifPresent(backgroundJobServerConfiguration::andScheduledJobsRequestSize);
            jobRunrRuntimeConfiguration.backgroundJobServer().orphanedJobsRequestSize().ifPresent(backgroundJobServerConfiguration::andOrphanedJobsRequestSize);
            jobRunrRuntimeConfiguration.backgroundJobServer().succeededJobRequestSize().ifPresent(backgroundJobServerConfiguration::andSucceededJobsRequestSize);
            jobRunrRuntimeConfiguration.backgroundJobServer().interruptJobsAwaitDurationOnStop().ifPresent(backgroundJobServerConfiguration::andInterruptJobsAwaitDurationOnStopBackgroundJobServer);
            return backgroundJobServerConfiguration;
        }
        return null;
    }

    @Produces
    @DefaultBean
    @Singleton
    BackgroundJobServer backgroundJobServer(StorageProvider storageProvider, JsonMapper jobRunrJsonMapper, JobActivator jobActivator, BackgroundJobServerConfiguration backgroundJobServerConfiguration, CarbonAwareJobManager carbonAwareJobManager) {
        if (jobRunrBuildTimeConfiguration.backgroundJobServer().enabled()) {
            int defaultNbrOfRetries = jobRunrRuntimeConfiguration.jobs().defaultNumberOfRetries().orElse(RetryFilter.DEFAULT_NBR_OF_RETRIES);
            int retryBackOffTimeSeed = jobRunrRuntimeConfiguration.jobs().retryBackOffTimeSeed().orElse(RetryFilter.DEFAULT_BACKOFF_POLICY_TIME_SEED);
            BackgroundJobServer backgroundJobServer = new BackgroundJobServer(storageProvider, carbonAwareJobManager, jobRunrJsonMapper, jobActivator, backgroundJobServerConfiguration);
            backgroundJobServer.setJobFilters(singletonList(new RetryFilter(defaultNbrOfRetries, retryBackOffTimeSeed)));
            return backgroundJobServer;
        }
        return null;
    }

    @Produces
    @DefaultBean
    @Singleton
    JobRunrDashboardWebServerConfiguration dashboardWebServerConfiguration() {
        if (jobRunrBuildTimeConfiguration.dashboard().enabled()) {
            final JobRunrDashboardWebServerConfiguration dashboardWebServerConfiguration = usingStandardDashboardConfiguration();
            jobRunrRuntimeConfiguration.dashboard().port().ifPresent(dashboardWebServerConfiguration::andPort);
            if (jobRunrRuntimeConfiguration.dashboard().username().isPresent() && jobRunrRuntimeConfiguration.dashboard().password().isPresent()) {
                dashboardWebServerConfiguration.andBasicAuthentication(jobRunrRuntimeConfiguration.dashboard().username().get(), jobRunrRuntimeConfiguration.dashboard().password().get());
            }
            dashboardWebServerConfiguration.andAllowAnonymousDataUsage(jobRunrRuntimeConfiguration.miscellaneous().allowAnonymousDataUsage());
            return dashboardWebServerConfiguration;
        }
        return null;
    }

    @Produces
    @DefaultBean
    @Singleton
    JobRunrDashboardWebServer dashboardWebServer(StorageProvider storageProvider, JsonMapper jobRunrJsonMapper, JobRunrDashboardWebServerConfiguration dashboardWebServerConfiguration) {
        if (jobRunrBuildTimeConfiguration.dashboard().enabled()) {
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
