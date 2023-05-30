package org.jobrunr.spring.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import jakarta.json.bind.Jsonb;
import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.jobrunr.dashboard.JobRunrDashboardWebServerConfiguration;
import org.jobrunr.jobs.details.JobDetailsGenerator;
import org.jobrunr.jobs.filters.RetryFilter;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.scheduling.RecurringJobPostProcessor;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.server.JobActivator;
import org.jobrunr.spring.autoconfigure.health.JobRunrHealthIndicator;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.gson.GsonJsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.jobrunr.utils.mapper.jsonb.JsonbJsonMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.jobrunr.dashboard.JobRunrDashboardWebServerConfiguration.usingStandardDashboardConfiguration;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.jobrunr.utils.reflection.ReflectionUtils.newInstance;

/**
 * A Spring Boot AutoConfiguration class for JobRunr
 */
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
@EnableConfigurationProperties(JobRunrProperties.class)
@ComponentScan(basePackages = {"org.jobrunr.scheduling"})
public class JobRunrAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "org.jobrunr.job-scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
    public JobScheduler jobScheduler(StorageProvider storageProvider, JobRunrProperties properties) {
        final JobDetailsGenerator jobDetailsGenerator = newInstance(properties.getJobScheduler().getJobDetailsGenerator());
        return new JobScheduler(storageProvider, jobDetailsGenerator, emptyList());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "org.jobrunr.job-scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
    public JobRequestScheduler jobRequestScheduler(StorageProvider storageProvider) {
        return new JobRequestScheduler(storageProvider, emptyList());
    }

    @Bean(destroyMethod = "stop")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "org.jobrunr.background-job-server", name = "enabled", havingValue = "true")
    public BackgroundJobServer backgroundJobServer(StorageProvider storageProvider, JsonMapper jobRunrJsonMapper, JobActivator jobActivator, BackgroundJobServerConfiguration backgroundJobServerConfiguration, JobRunrProperties properties) {
        final BackgroundJobServer backgroundJobServer = new BackgroundJobServer(storageProvider, jobRunrJsonMapper, jobActivator, backgroundJobServerConfiguration);
        backgroundJobServer.setJobFilters(singletonList(new RetryFilter(properties.getJobs().getDefaultNumberOfRetries(), properties.getJobs().getRetryBackOffTimeSeed())));
        backgroundJobServer.start();
        return backgroundJobServer;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "org.jobrunr.background-job-server", name = "enabled", havingValue = "true")
    public BackgroundJobServerConfiguration backgroundJobServerConfiguration(JobRunrProperties properties) {
        PropertyMapper map = PropertyMapper.get();
        BackgroundJobServerConfiguration backgroundJobServerConfiguration = usingStandardBackgroundJobServerConfiguration();
        JobRunrProperties.BackgroundJobServer backgroundJobServerProperties = properties.getBackgroundJobServer();

        map.from(backgroundJobServerProperties::getName).whenNonNull().to(backgroundJobServerConfiguration::andName);
        map.from(backgroundJobServerProperties::getWorkerCount).whenNonNull().to(backgroundJobServerConfiguration::andWorkerCount);
        map.from(backgroundJobServerProperties::getPollIntervalInSeconds).to(backgroundJobServerConfiguration::andPollIntervalInSeconds);
        map.from(backgroundJobServerProperties::getDeleteSucceededJobsAfter).to(backgroundJobServerConfiguration::andDeleteSucceededJobsAfter);
        map.from(backgroundJobServerProperties::getPermanentlyDeleteDeletedJobsAfter).to(backgroundJobServerConfiguration::andPermanentlyDeleteDeletedJobsAfter);
        map.from(backgroundJobServerProperties::getScheduledJobsRequestSize).to(backgroundJobServerConfiguration::andScheduledJobsRequestSize);
        map.from(backgroundJobServerProperties::getOrphanedJobsRequestSize).to(backgroundJobServerConfiguration::andOrphanedJobsRequestSize);
        map.from(backgroundJobServerProperties::getSucceededJobsRequestSize).to(backgroundJobServerConfiguration::andSucceededJobsRequestSize);

        return backgroundJobServerConfiguration;
    }

    @Bean(destroyMethod = "stop")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "org.jobrunr.dashboard", name = "enabled", havingValue = "true")
    public JobRunrDashboardWebServer dashboardWebServer(StorageProvider storageProvider, JsonMapper jobRunrJsonMapper, JobRunrDashboardWebServerConfiguration dashboardWebServerConfiguration) {
        JobRunrDashboardWebServer dashboardWebServer = new JobRunrDashboardWebServer(storageProvider, jobRunrJsonMapper, dashboardWebServerConfiguration);
        dashboardWebServer.start();
        return dashboardWebServer;
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

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
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
