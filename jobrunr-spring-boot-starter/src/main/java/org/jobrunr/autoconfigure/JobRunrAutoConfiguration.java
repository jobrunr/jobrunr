package org.jobrunr.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.jobrunr.dashboard.JobRunrDashboardWebServerConfiguration;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.server.JobActivator;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.gson.GsonJsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.jobrunr.utils.mapper.jsonb.JsonbJsonMapper;
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
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "org.jobrunr.background-job-server", name = "enabled", havingValue = "true")
    public BackgroundJobServer backgroundJobServer(StorageProvider storageProvider, JobActivator jobActivator, BackgroundJobServerConfiguration backgroundJobServerConfiguration) {
        final BackgroundJobServer backgroundJobServer = new BackgroundJobServer(storageProvider, jobActivator, backgroundJobServerConfiguration);
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

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "org.jobrunr.dashboard", name = "enabled", havingValue = "true")
    public JobRunrDashboardWebServer dashboardWebServer(StorageProvider storageProvider, JsonMapper jsonMapper, JobRunrDashboardWebServerConfiguration dashboardWebServerConfiguration) {
        JobRunrDashboardWebServer dashboardWebServer = new JobRunrDashboardWebServer(storageProvider, jsonMapper, dashboardWebServerConfiguration);
        dashboardWebServer.start();
        return dashboardWebServer;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "org.jobrunr.dashboard", name = "enabled", havingValue = "true")
    public JobRunrDashboardWebServerConfiguration dashboardWebServerConfiguration(JobRunrProperties properties) {
        return usingStandardDashboardConfiguration()
                .andPort(properties.getDashboard().getPort());
    }

    @Bean
    @ConditionalOnMissingBean
    public JobActivator jobActivator(ApplicationContext applicationContext) {
        return applicationContext::getBean;
    }


    @Bean
    @ConditionalOnMissingBean
    public JobMapper jobMapper(JsonMapper jsonMapper) {
        return new JobMapper(jsonMapper);
    }

    @Configuration
    @ConditionalOnClass(Gson.class)
    public class JobRunrGsonAutoConfiguration {

        @Bean(name = "jsonMapper")
        @ConditionalOnMissingBean
        public JsonMapper gsonJsonMapper() {
            return new GsonJsonMapper();
        }
    }

    @Configuration
    @ConditionalOnClass(ObjectMapper.class)
    public class JobRunrJacksonAutoConfiguration {

        @Bean(name = "jsonMapper")
        @ConditionalOnMissingBean
        public JsonMapper jacksonJsonMapper() {
            return new JacksonJsonMapper();
        }

    }

    @ConditionalOnClass(Jsonb.class)
    @ConditionalOnResource(resources = {"classpath:META-INF/services/javax.json.bind.spi.JsonbProvider",
            "classpath:META-INF/services/javax.json.spi.JsonProvider"})
    public class JobRunrJsonbAutoConfiguration {

        @Bean(name = "jsonMapper")
        @ConditionalOnMissingBean
        public JsonMapper jsonbJsonMapper() {
            return new JsonbJsonMapper();
        }
    }

}
