package org.jobrunr.spring.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import jakarta.json.bind.Jsonb;
import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.jobrunr.dashboard.JobRunrDashboardWebServerConfiguration;
import org.jobrunr.jobs.carbonaware.CarbonAwareConfiguration;
import org.jobrunr.jobs.carbonaware.CarbonAwareJobManager;
import org.jobrunr.jobs.details.JobDetailsGenerator;
import org.jobrunr.jobs.filters.RetryFilter;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.scheduling.RecurringJobPostProcessor;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.server.JobActivator;
import org.jobrunr.server.configuration.BackgroundJobServerThreadType;
import org.jobrunr.server.configuration.BackgroundJobServerWorkerPolicy;
import org.jobrunr.server.configuration.DefaultBackgroundJobServerWorkerPolicy;
import org.jobrunr.spring.autoconfigure.health.JobRunrHealthIndicator;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.gson.GsonJsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.jobrunr.utils.mapper.jsonb.JsonbJsonMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.jobrunr.dashboard.JobRunrDashboardWebServerConfiguration.usingStandardDashboardConfiguration;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.jobrunr.utils.reflection.ReflectionUtils.newInstance;

/**
 * A Spring Boot AutoConfiguration class for JobRunr
 */
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@AutoConfiguration
@EnableConfigurationProperties(JobRunrProperties.class)
@ComponentScan(basePackages = {"org.jobrunr.scheduling"})
public class JobRunrAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "org.jobrunr.job-scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
    public CarbonAwareJobManager carbonAwareJobManager(JobRunrProperties jobRunrProperties, JsonMapper jobRunrJsonMapper) {
        PropertyMapper map = PropertyMapper.get();

        CarbonAwareConfiguration carbonAwareConfiguration = CarbonAwareConfiguration.usingStandardCarbonAwareConfiguration();
        JobRunrProperties.CarbonAware carbonAwareProperties = jobRunrProperties.getJobs().getCarbonAware();

        map.from(carbonAwareProperties::getAreaCode).whenNonNull().to(carbonAwareConfiguration::andAreaCode);
        map.from(carbonAwareProperties::getApiClientConnectTimeoutMs).whenNonNull().to(connectTimeout -> carbonAwareConfiguration.andApiClientConnectTimeout(Duration.ofMillis(connectTimeout)));
        map.from(carbonAwareProperties::getApiClientReadTimeoutMs).whenNonNull().to(readTimeout -> carbonAwareConfiguration.andApiClientReadTimeout(Duration.ofMillis(readTimeout)));
        return new CarbonAwareJobManager(carbonAwareConfiguration, jobRunrJsonMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "org.jobrunr.job-scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
    public JobScheduler jobScheduler(StorageProvider storageProvider, JobRunrProperties properties, CarbonAwareJobManager carbonAwareJobManager) {
        final JobDetailsGenerator jobDetailsGenerator = newInstance(properties.getJobScheduler().getJobDetailsGenerator());
        return new JobScheduler(storageProvider, carbonAwareJobManager, jobDetailsGenerator, emptyList());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "org.jobrunr.job-scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
    public JobRequestScheduler jobRequestScheduler(StorageProvider storageProvider, CarbonAwareJobManager carbonAwareJobManager) {
        return new JobRequestScheduler(storageProvider, carbonAwareJobManager, emptyList());
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "org.jobrunr.background-job-server", name = "enabled", havingValue = "true")
    public BackgroundJobServer backgroundJobServer(StorageProvider storageProvider, JsonMapper jobRunrJsonMapper, JobActivator jobActivator, BackgroundJobServerConfiguration backgroundJobServerConfiguration, JobRunrProperties properties, CarbonAwareJobManager carbonAwareJobManager) {
        final BackgroundJobServer backgroundJobServer = new BackgroundJobServer(storageProvider, carbonAwareJobManager, jobRunrJsonMapper, jobActivator, backgroundJobServerConfiguration);
        backgroundJobServer.setJobFilters(singletonList(new RetryFilter(properties.getJobs().getDefaultNumberOfRetries(), properties.getJobs().getRetryBackOffTimeSeed())));
        return backgroundJobServer;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "org.jobrunr.background-job-server", name = "enabled", havingValue = "true")
    public BackgroundJobServerWorkerPolicy backgroundJobServerWorkerPolicy(JobRunrProperties properties) {
        JobRunrProperties.BackgroundJobServer backgroundJobServerProperties = properties.getBackgroundJobServer();
        BackgroundJobServerThreadType threadType = ofNullable(backgroundJobServerProperties.getThreadType()).orElse(BackgroundJobServerThreadType.getDefaultThreadType());
        int workerCount = ofNullable(backgroundJobServerProperties.getWorkerCount()).orElse(threadType.getDefaultWorkerCount());
        return new DefaultBackgroundJobServerWorkerPolicy(workerCount, threadType);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "org.jobrunr.background-job-server", name = "enabled", havingValue = "true")
    public BackgroundJobServerConfiguration backgroundJobServerConfiguration(JobRunrProperties properties, BackgroundJobServerWorkerPolicy backgroundJobServerWorkerPolicy) {
        PropertyMapper map = PropertyMapper.get();
        BackgroundJobServerConfiguration backgroundJobServerConfiguration = usingStandardBackgroundJobServerConfiguration();
        JobRunrProperties.BackgroundJobServer backgroundJobServerProperties = properties.getBackgroundJobServer();

        backgroundJobServerConfiguration.andBackgroundJobServerWorkerPolicy(backgroundJobServerWorkerPolicy);

        map.from(backgroundJobServerProperties::getName).whenNonNull().to(backgroundJobServerConfiguration::andName);
        map.from(backgroundJobServerProperties::getPollIntervalInSeconds).to(backgroundJobServerConfiguration::andPollIntervalInSeconds);
        map.from(backgroundJobServerProperties::getDeleteSucceededJobsAfter).to(backgroundJobServerConfiguration::andDeleteSucceededJobsAfter);
        map.from(backgroundJobServerProperties::getPermanentlyDeleteDeletedJobsAfter).to(backgroundJobServerConfiguration::andPermanentlyDeleteDeletedJobsAfter);
        map.from(backgroundJobServerProperties::getScheduledJobsRequestSize).to(backgroundJobServerConfiguration::andScheduledJobsRequestSize);
        map.from(backgroundJobServerProperties::getCarbonAwaitingJobsRequestSize).to(backgroundJobServerConfiguration::andCarbonAwaitingJobsRequestSize);
        map.from(backgroundJobServerProperties::getOrphanedJobsRequestSize).to(backgroundJobServerConfiguration::andOrphanedJobsRequestSize);
        map.from(backgroundJobServerProperties::getSucceededJobsRequestSize).to(backgroundJobServerConfiguration::andSucceededJobsRequestSize);
        map.from(backgroundJobServerProperties::getInterruptJobsAwaitDurationOnStop).to(backgroundJobServerConfiguration::andInterruptJobsAwaitDurationOnStopBackgroundJobServer);

        return backgroundJobServerConfiguration;
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "org.jobrunr.dashboard", name = "enabled", havingValue = "true")
    public JobRunrDashboardWebServer dashboardWebServer(StorageProvider storageProvider, JsonMapper jobRunrJsonMapper, JobRunrDashboardWebServerConfiguration dashboardWebServerConfiguration) {
        return new JobRunrDashboardWebServer(storageProvider, jobRunrJsonMapper, dashboardWebServerConfiguration);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "org.jobrunr.dashboard", name = "enabled", havingValue = "true")
    public JobRunrDashboardWebServerConfiguration dashboardWebServerConfiguration(JobRunrProperties properties) {
        return usingStandardDashboardConfiguration()
                .andPort(properties.getDashboard().getPort())
                .andBasicAuthentication(properties.getDashboard().getUsername(), properties.getDashboard().getPassword())
                .andAllowAnonymousDataUsage(properties.getMiscellaneous().isAllowAnonymousDataUsage());
    }

    @Bean
    @ConditionalOnMissingBean
    public JobActivator jobActivator(ApplicationContext applicationContext) {
        return applicationContext::getBean;
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

    @Configuration
    @ConditionalOnClass(Gson.class)
    public static class JobRunrGsonAutoConfiguration {

        @Bean(name = "jobRunrJsonMapper")
        @ConditionalOnMissingBean
        public JsonMapper gsonJsonMapper() {
            return new GsonJsonMapper();
        }
    }

    @Configuration
    @ConditionalOnClass(ObjectMapper.class)
    public static class JobRunrJacksonAutoConfiguration {

        @Bean(name = "jobRunrJsonMapper")
        @ConditionalOnMissingBean
        public JsonMapper jacksonJsonMapper() {
            return new JacksonJsonMapper();
        }

    }

    @ConditionalOnClass(Jsonb.class)
    @ConditionalOnResource(resources = {"classpath:META-INF/services/javax.json.bind.spi.JsonbProvider",
            "classpath:META-INF/services/javax.json.spi.JsonProvider"})
    public static class JobRunrJsonbAutoConfiguration {

        @Bean(name = "jobRunrJsonMapper")
        @ConditionalOnMissingBean
        public JsonMapper jsonbJsonMapper() {
            return new JsonbJsonMapper();
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
