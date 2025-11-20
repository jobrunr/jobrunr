package org.jobrunr.spring.autoconfigure;

import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.jobrunr.dashboard.JobRunrDashboardWebServerConfiguration;
import org.jobrunr.jobs.details.JobDetailsGenerator;
import org.jobrunr.jobs.filters.RetryFilter;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.kotlin.utils.mapper.KotlinxSerializationJsonMapper;
import org.jobrunr.scheduling.AsyncJobPostProcessor;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.scheduling.RecurringJobPostProcessor;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.server.JobActivator;
import org.jobrunr.server.JobActivatorShutdownException;
import org.jobrunr.server.carbonaware.CarbonAwareJobProcessingConfiguration;
import org.jobrunr.server.configuration.BackgroundJobServerThreadType;
import org.jobrunr.server.configuration.BackgroundJobServerWorkerPolicy;
import org.jobrunr.server.configuration.DefaultBackgroundJobServerWorkerPolicy;
import org.jobrunr.spring.autoconfigure.health.JobRunrHealthIndicator;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.JsonMapperFactory;
import org.jobrunr.utils.reflection.ReflectionUtils;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanCreationNotAllowedException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;

import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.jobrunr.dashboard.JobRunrDashboardWebServerConfiguration.usingStandardDashboardConfiguration;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;

/**
 * A Spring Boot AutoConfiguration class for JobRunr
 */
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@AutoConfiguration
@EnableConfigurationProperties(JobRunrProperties.class)
@ComponentScan(basePackages = {"org.jobrunr.scheduling"})
public class JobRunrAutoConfiguration {

    @Bean
    public JobRunrStarter jobRunrStarter(Optional<BackgroundJobServer> backgroundJobServer, Optional<JobRunrDashboardWebServer> webServer) {
        return new JobRunrStarter(backgroundJobServer, webServer);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "jobrunr.job-scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
    public JobScheduler jobScheduler(StorageProvider storageProvider, JobRunrProperties properties) {
        final JobDetailsGenerator jobDetailsGenerator = ReflectionUtils.newInstance(properties.getJobScheduler().getJobDetailsGenerator());
        return new JobScheduler(storageProvider, jobDetailsGenerator, emptyList());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "jobrunr.job-scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
    public JobRequestScheduler jobRequestScheduler(StorageProvider storageProvider) {
        return new JobRequestScheduler(storageProvider, emptyList());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "jobrunr.background-job-server", name = "enabled", havingValue = "true")
    public BackgroundJobServer backgroundJobServer(StorageProvider storageProvider, JsonMapper jobRunrJsonMapper, JobActivator jobActivator, BackgroundJobServerConfiguration backgroundJobServerConfiguration, JobRunrProperties properties) {
        final BackgroundJobServer backgroundJobServer = new BackgroundJobServer(storageProvider, jobRunrJsonMapper, jobActivator, backgroundJobServerConfiguration);
        backgroundJobServer.setJobFilters(singletonList(new RetryFilter(properties.getJobs().getDefaultNumberOfRetries(), properties.getJobs().getRetryBackOffTimeSeed())));
        return backgroundJobServer;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "jobrunr.background-job-server", name = "enabled", havingValue = "true")
    public BackgroundJobServerWorkerPolicy backgroundJobServerWorkerPolicy(JobRunrProperties properties) {
        JobRunrProperties.BackgroundJobServer backgroundJobServerProperties = properties.getBackgroundJobServer();
        BackgroundJobServerThreadType threadType = ofNullable(backgroundJobServerProperties.getThreadType()).orElse(BackgroundJobServerThreadType.getDefaultThreadType());
        int workerCount = ofNullable(backgroundJobServerProperties.getWorkerCount()).orElse(threadType.getDefaultWorkerCount());
        return new DefaultBackgroundJobServerWorkerPolicy(workerCount, threadType);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "jobrunr.background-job-server", name = "enabled", havingValue = "true")
    public BackgroundJobServerConfiguration backgroundJobServerConfiguration(JobRunrProperties properties, BackgroundJobServerWorkerPolicy backgroundJobServerWorkerPolicy) {
        PropertyMapper map = PropertyMapper.get();
        BackgroundJobServerConfiguration backgroundJobServerConfiguration = usingStandardBackgroundJobServerConfiguration();
        JobRunrProperties.BackgroundJobServer backgroundJobServerProperties = properties.getBackgroundJobServer();

        backgroundJobServerConfiguration.andBackgroundJobServerWorkerPolicy(backgroundJobServerWorkerPolicy);

        map.from(backgroundJobServerProperties::getName).whenNonNull().to(backgroundJobServerConfiguration::andName);
        map.from(backgroundJobServerProperties::getPollIntervalInSeconds).to(backgroundJobServerConfiguration::andPollIntervalInSeconds);
        map.from(backgroundJobServerProperties::getServerTimeoutPollIntervalMultiplicand).to(backgroundJobServerConfiguration::andServerTimeoutPollIntervalMultiplicand);
        map.from(backgroundJobServerProperties::getDeleteSucceededJobsAfter).to(backgroundJobServerConfiguration::andDeleteSucceededJobsAfter);
        map.from(backgroundJobServerProperties::getPermanentlyDeleteDeletedJobsAfter).to(backgroundJobServerConfiguration::andPermanentlyDeleteDeletedJobsAfter);
        map.from(backgroundJobServerProperties::getScheduledJobsRequestSize).to(backgroundJobServerConfiguration::andScheduledJobsRequestSize);
        map.from(backgroundJobServerProperties::getCarbonAwaitingJobsRequestSize).to(backgroundJobServerConfiguration::andCarbonAwaitingJobsRequestSize);
        map.from(backgroundJobServerProperties::getOrphanedJobsRequestSize).to(backgroundJobServerConfiguration::andOrphanedJobsRequestSize);
        map.from(backgroundJobServerProperties::getSucceededJobsRequestSize).to(backgroundJobServerConfiguration::andSucceededJobsRequestSize);
        map.from(backgroundJobServerProperties::getInterruptJobsAwaitDurationOnStop).to(backgroundJobServerConfiguration::andInterruptJobsAwaitDurationOnStopBackgroundJobServer);

        CarbonAwareJobProcessingConfiguration carbonAwareJobProcessingConfiguration = CarbonAwareJobProcessingConfiguration.usingDisabledCarbonAwareJobProcessingConfiguration();
        JobRunrProperties.CarbonAwareJobProcessing carbonAwareJobProcessingProperties = properties.getBackgroundJobServer().getCarbonAwareJobProcessing();
        map.from(carbonAwareJobProcessingProperties::isEnabled).to(carbonAwareJobProcessingConfiguration::andCarbonAwareSchedulingEnabled);
        map.from(carbonAwareJobProcessingProperties::getDataProvider).whenNonNull().to(carbonAwareJobProcessingConfiguration::andDataProvider);
        map.from(carbonAwareJobProcessingProperties::getAreaCode).whenNonNull().to(carbonAwareJobProcessingConfiguration::andAreaCode);
        map.from(carbonAwareJobProcessingProperties::getExternalCode).whenNonNull().to(carbonAwareJobProcessingConfiguration::andExternalCode);
        map.from(carbonAwareJobProcessingProperties::getExternalIdentifier).whenNonNull().to(carbonAwareJobProcessingConfiguration::andExternalIdentifier);
        map.from(carbonAwareJobProcessingProperties::getApiClientConnectTimeout).whenNonNull().to(carbonAwareJobProcessingConfiguration::andApiClientConnectTimeout);
        map.from(carbonAwareJobProcessingProperties::getApiClientReadTimeout).whenNonNull().to(carbonAwareJobProcessingConfiguration::andApiClientReadTimeout);
        map.from(carbonAwareJobProcessingProperties::getPollIntervalInMinutes).to(carbonAwareJobProcessingConfiguration::andPollIntervalInMinutes);
        backgroundJobServerConfiguration.andCarbonAwareJobProcessingConfiguration(carbonAwareJobProcessingConfiguration);

        return backgroundJobServerConfiguration;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "jobrunr.dashboard", name = "enabled", havingValue = "true")
    public JobRunrDashboardWebServer dashboardWebServer(StorageProvider storageProvider, JsonMapper jobRunrJsonMapper, JobRunrDashboardWebServerConfiguration dashboardWebServerConfiguration) {
        return new JobRunrDashboardWebServer(storageProvider, jobRunrJsonMapper, dashboardWebServerConfiguration);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "jobrunr.dashboard", name = "enabled", havingValue = "true")
    public JobRunrDashboardWebServerConfiguration dashboardWebServerConfiguration(JobRunrProperties properties) {
        return usingStandardDashboardConfiguration()
                .andPort(properties.getDashboard().getPort())
                .andBasicAuthentication(properties.getDashboard().getUsername(), properties.getDashboard().getPassword())
                .andAllowAnonymousDataUsage(properties.getMiscellaneous().isAllowAnonymousDataUsage());
    }

    @Bean
    @ConditionalOnMissingBean
    public JobActivator jobActivator(ApplicationContext applicationContext) {
        return new JobActivator() {
            @Override
            public <T> T activateJob(Class<T> type) throws JobActivatorShutdownException {
                try {
                    return applicationContext.getBean(type);
                } catch (BeanCreationNotAllowedException e) {
                    throw new JobActivatorShutdownException("Spring IoC container is shutting down", e);
                }
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public JobMapper jobMapper(JsonMapper jobRunrJsonMapper) {
        return new JobMapper(jobRunrJsonMapper);
    }

    @Bean
    @ConditionalOnBean(JobScheduler.class)
    public static RecurringJobPostProcessor recurringJobPostProcessor() {
        return new RecurringJobPostProcessor();
    }

    @Bean
    @ConditionalOnBean(JobScheduler.class)
    public static AsyncJobPostProcessor asyncJobPostProcessor() {
        return new AsyncJobPostProcessor();
    }

    @Configuration
    @ConditionalOnMissingClass(value = {"kotlinx.serialization.json.Json"})
    public static class JobRunrJsonMapperAutoConfiguration implements BeanClassLoaderAware {

        @Override
        public void setBeanClassLoader(ClassLoader classLoader) {
            JsonMapperFactory.setJsonMapperClassPresentFunction(s -> ClassUtils.isPresent(s, classLoader));
        }

        @Bean(name = "jobRunrJsonMapper")
        @ConditionalOnMissingBean
        public JsonMapper jobRunrJsonMapper() {
            return JsonMapperFactory.createJsonMapper();
        }
    }

    @Configuration
    @ConditionalOnClass(value = {kotlinx.serialization.json.Json.class, KotlinxSerializationJsonMapper.class})
    public static class JobRunrKotlinxSerializationJsonMapperAutoConfiguration {

        @Bean(name = "jobRunrJsonMapper")
        @ConditionalOnMissingBean
        public JsonMapper kotlinxSerializationJsonMapper() {
            return new KotlinxSerializationJsonMapper();
        }
    }

    @Configuration
    @ConditionalOnClass(HealthIndicator.class)
    @ConditionalOnEnabledHealthIndicator("jobrunr")
    public static class JobRunrHealthIndicatorAutoConfiguration {

        @Bean(name = "JobRunr")
        public HealthIndicator healthIndicator(JobRunrProperties properties, ObjectProvider<BackgroundJobServer> backgroundJobServerProvider) {
            return new JobRunrHealthIndicator(properties, backgroundJobServerProvider);
        }
    }

}
