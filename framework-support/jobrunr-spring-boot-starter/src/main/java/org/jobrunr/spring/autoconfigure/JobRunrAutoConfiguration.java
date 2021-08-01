package org.jobrunr.spring.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.jobrunr.dashboard.JobRunrDashboardWebServerConfiguration;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.scheduling.BackgroundJob;
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
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.json.bind.Jsonb;

import static org.jobrunr.dashboard.JobRunrDashboardWebServerConfiguration.usingStandardDashboardConfiguration;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;

/**
 * A Spring Boot AutoConfiguration class for JobRunr
 */
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
@EnableConfigurationProperties(JobRunrProperties.class)
public class JobRunrAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "org.jobrunr.job-scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
    public JobScheduler jobScheduler(StorageProvider storageProvider) {
        final JobScheduler jobScheduler = new JobScheduler(storageProvider);
        BackgroundJob.setJobScheduler(jobScheduler);
        return jobScheduler;
    }

    @Bean
    public RecurringJobPostProcessor recurringJobPostProcessor(JobScheduler jobScheduler) {
        return new RecurringJobPostProcessor(jobScheduler);
    }

    @Bean(destroyMethod = "stop")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "org.jobrunr.background-job-server", name = "enabled", havingValue = "true")
    public BackgroundJobServer backgroundJobServer(StorageProvider storageProvider, JsonMapper jobRunrJsonMapper, JobActivator jobActivator, BackgroundJobServerConfiguration backgroundJobServerConfiguration) {
        final BackgroundJobServer backgroundJobServer = new BackgroundJobServer(storageProvider, jobRunrJsonMapper, jobActivator, backgroundJobServerConfiguration);
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

        map.from(backgroundJobServerProperties::getWorkerCount).whenNonNull().to(backgroundJobServerConfiguration::andWorkerCount);
        map.from(backgroundJobServerProperties::getPollIntervalInSeconds).to(backgroundJobServerConfiguration::andPollIntervalInSeconds);
        map.from(backgroundJobServerProperties::getDeleteSucceededJobsAfter).to(backgroundJobServerConfiguration::andDeleteSucceededJobsAfter);
        map.from(backgroundJobServerProperties::getPermanentlyDeleteDeletedJobsAfter).to(backgroundJobServerConfiguration::andPermanentlyDeleteDeletedJobsAfter);

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
                .andBasicAuthentication(properties.getDashboard().getUsername(), properties.getDashboard().getPassword());
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
    public static class JobRunrHealthIndicatorAutoConfiguration {

        @Bean
        public HealthIndicator healthIndicator(JobRunrProperties properties, ObjectProvider<BackgroundJobServer> backgroundJobServerProvider) {
            return new JobRunrHealthIndicator(properties, backgroundJobServerProvider);
        }
    }

}
