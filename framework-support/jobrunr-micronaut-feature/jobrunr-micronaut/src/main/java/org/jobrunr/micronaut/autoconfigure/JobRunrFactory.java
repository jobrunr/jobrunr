package org.jobrunr.micronaut.autoconfigure;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
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
import org.jobrunr.server.carbonaware.CarbonAwareJobProcessingConfiguration;
import org.jobrunr.server.configuration.BackgroundJobServerThreadType;
import org.jobrunr.server.configuration.BackgroundJobServerWorkerPolicy;
import org.jobrunr.server.configuration.DefaultBackgroundJobServerWorkerPolicy;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.JsonMapperFactory;
import org.jobrunr.utils.reflection.ReflectionUtils;

import java.time.Duration;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.jobrunr.dashboard.JobRunrDashboardWebServerConfiguration.usingStandardDashboardConfiguration;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;

@Factory
public class JobRunrFactory {

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private JobRunrConfiguration configuration;

    @Singleton
    @Requires(property = "jobrunr.job-scheduler.enabled", value = "true")
    public JobScheduler jobScheduler(StorageProvider storageProvider) {
        final JobDetailsGenerator jobDetailsGenerator = ReflectionUtils.newInstance(configuration.getJobScheduler().getJobDetailsGenerator().orElse(CachingJobDetailsGenerator.class.getName()));
        return new JobScheduler(storageProvider, jobDetailsGenerator, emptyList());
    }

    @Singleton
    @Requires(property = "jobrunr.job-scheduler.enabled", value = "true")
    public JobRequestScheduler jobRequestScheduler(StorageProvider storageProvider) {
        return new JobRequestScheduler(storageProvider, emptyList());
    }

    @Singleton
    @Requires(property = "jobrunr.background-job-server.enabled", value = "true")
    public BackgroundJobServerWorkerPolicy backgroundJobServerWorkerPolicy() {
        BackgroundJobServerThreadType threadType = configuration.getBackgroundJobServer().getThreadType().orElse(BackgroundJobServerThreadType.getDefaultThreadType());
        int workerCount = configuration.getBackgroundJobServer().getWorkerCount().orElse(threadType.getDefaultWorkerCount());
        return new DefaultBackgroundJobServerWorkerPolicy(workerCount, threadType);
    }

    @Singleton
    @Requires(property = "jobrunr.background-job-server.enabled", value = "true")
    public BackgroundJobServerConfiguration backgroundJobServerConfiguration(BackgroundJobServerWorkerPolicy backgroundJobServerWorkerPolicy) {
        final BackgroundJobServerConfiguration backgroundJobServerConfiguration = usingStandardBackgroundJobServerConfiguration();

        backgroundJobServerConfiguration.andBackgroundJobServerWorkerPolicy(backgroundJobServerWorkerPolicy);

        configuration.getBackgroundJobServer().getName().ifPresent(backgroundJobServerConfiguration::andName);
        configuration.getBackgroundJobServer().getPollIntervalInSeconds().ifPresent(backgroundJobServerConfiguration::andPollIntervalInSeconds);
        configuration.getBackgroundJobServer().getServerTimeoutPollIntervalMultiplicand().ifPresent(backgroundJobServerConfiguration::andServerTimeoutPollIntervalMultiplicand);
        configuration.getBackgroundJobServer().getDeleteSucceededJobsAfter().ifPresent(backgroundJobServerConfiguration::andDeleteSucceededJobsAfter);
        configuration.getBackgroundJobServer().getPermanentlyDeleteDeletedJobsAfter().ifPresent(backgroundJobServerConfiguration::andPermanentlyDeleteDeletedJobsAfter);
        configuration.getBackgroundJobServer().getScheduledJobsRequestSize().ifPresent(backgroundJobServerConfiguration::andScheduledJobsRequestSize);
        configuration.getBackgroundJobServer().getCarbonAwaitingJobsRequestSize().ifPresent(backgroundJobServerConfiguration::andCarbonAwaitingJobsRequestSize);
        configuration.getBackgroundJobServer().getOrphanedJobsRequestSize().ifPresent(backgroundJobServerConfiguration::andOrphanedJobsRequestSize);
        configuration.getBackgroundJobServer().getSucceededJobsRequestSize().ifPresent(backgroundJobServerConfiguration::andSucceededJobsRequestSize);
        configuration.getBackgroundJobServer().getInterruptJobsAwaitDurationOnStop().ifPresent(backgroundJobServerConfiguration::andInterruptJobsAwaitDurationOnStopBackgroundJobServer);

        CarbonAwareJobProcessingConfiguration carbonAwareJobProcessingConfiguration = CarbonAwareJobProcessingConfiguration.usingDisabledCarbonAwareJobProcessingConfiguration();
        carbonAwareJobProcessingConfiguration.andCarbonAwareSchedulingEnabled(configuration.getBackgroundJobServer().getCarbonAwareJobProcessingConfiguration().isEnabled());
        configuration.getBackgroundJobServer().getCarbonAwareJobProcessingConfiguration().getAreaCode().ifPresent(carbonAwareJobProcessingConfiguration::andAreaCode);
        configuration.getBackgroundJobServer().getCarbonAwareJobProcessingConfiguration().getDataProvider().ifPresent(carbonAwareJobProcessingConfiguration::andDataProvider);
        configuration.getBackgroundJobServer().getCarbonAwareJobProcessingConfiguration().getExternalCode().ifPresent(carbonAwareJobProcessingConfiguration::andExternalCode);
        configuration.getBackgroundJobServer().getCarbonAwareJobProcessingConfiguration().getExternalIdentifier().ifPresent(carbonAwareJobProcessingConfiguration::andExternalIdentifier);
        configuration.getBackgroundJobServer().getCarbonAwareJobProcessingConfiguration().getApiClientConnectTimeoutMs().ifPresent(connectTimeout -> carbonAwareJobProcessingConfiguration.andApiClientConnectTimeout(Duration.ofMillis(connectTimeout)));
        configuration.getBackgroundJobServer().getCarbonAwareJobProcessingConfiguration().getApiClientReadTimeoutMs().ifPresent(readTimeout -> carbonAwareJobProcessingConfiguration.andApiClientReadTimeout(Duration.ofMillis(readTimeout)));
        configuration.getBackgroundJobServer().getCarbonAwareJobProcessingConfiguration().getPollIntervalInMinutes().ifPresent(carbonAwareJobProcessingConfiguration::andPollIntervalInMinutes);
        backgroundJobServerConfiguration.andCarbonAwareJobProcessingConfiguration(carbonAwareJobProcessingConfiguration);
        return backgroundJobServerConfiguration;
    }

    @Singleton
    @Requires(property = "jobrunr.background-job-server.enabled", value = "true")
    public BackgroundJobServer backgroundJobServer(StorageProvider storageProvider, JsonMapper jobRunrJsonMapper, JobActivator jobActivator, BackgroundJobServerConfiguration backgroundJobServerConfiguration) {
        int defaultNbrOfRetries = configuration.getJobs().getDefaultNumberOfRetries().orElse(RetryFilter.DEFAULT_NBR_OF_RETRIES);
        int retryBackOffTimeSeed = configuration.getJobs().getRetryBackOffTimeSeed().orElse(RetryFilter.DEFAULT_BACKOFF_POLICY_TIME_SEED);
        BackgroundJobServer backgroundJobServer = new BackgroundJobServer(storageProvider, jobRunrJsonMapper, jobActivator, backgroundJobServerConfiguration);
        backgroundJobServer.setJobFilters(singletonList(new RetryFilter(defaultNbrOfRetries, retryBackOffTimeSeed)));
        return backgroundJobServer;
    }

    @Singleton
    @Requires(property = "jobrunr.dashboard.enabled", value = "true")
    public JobRunrDashboardWebServerConfiguration dashboardWebServerConfiguration() {
        final JobRunrDashboardWebServerConfiguration dashboardWebServerConfiguration = usingStandardDashboardConfiguration();
        configuration.getDashboard().getPort().ifPresent(dashboardWebServerConfiguration::andPort);
        if (configuration.getDashboard().getUsername().isPresent() && configuration.getDashboard().getPassword().isPresent()) {
            dashboardWebServerConfiguration.andBasicAuthentication(configuration.getDashboard().getUsername().get(), configuration.getDashboard().getPassword().get());
        }
        dashboardWebServerConfiguration.andAllowAnonymousDataUsage(configuration.getMiscellaneous().isAllowAnonymousDataUsage());
        return dashboardWebServerConfiguration;
    }

    @Singleton
    @Requires(property = "jobrunr.dashboard.enabled", value = "true")
    public JobRunrDashboardWebServer dashboardWebServer(StorageProvider storageProvider, JsonMapper jobRunrJsonMapper, JobRunrDashboardWebServerConfiguration dashboardWebServerConfiguration) {
        return new JobRunrDashboardWebServer(storageProvider, jobRunrJsonMapper, dashboardWebServerConfiguration);
    }

    @Singleton
    public JobActivator jobActivator() {
        return new JobActivator() {
            @Override
            public <T> T activateJob(Class<T> aClass) {
                return applicationContext.getBean(aClass);
            }
        };
    }

    @Singleton
    public JobMapper jobMapper(JsonMapper jobRunrJsonMapper) {
        return new JobMapper(jobRunrJsonMapper);
    }

    @Singleton
    public JsonMapper jobRunrJsonMapper() {
        return JsonMapperFactory.createJsonMapper();
    }
}
