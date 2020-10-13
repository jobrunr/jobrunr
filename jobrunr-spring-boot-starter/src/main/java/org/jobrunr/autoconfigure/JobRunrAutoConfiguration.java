package org.jobrunr.autoconfigure;

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
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
        BackgroundJobServerConfiguration backgroundJobServerConfiguration = usingStandardBackgroundJobServerConfiguration();
        if (properties.getBackgroundJobServer().getWorkerCount() != null) {
            backgroundJobServerConfiguration.andWorkerCount(properties.getBackgroundJobServer().getWorkerCount());
        }

        if (properties.getBackgroundJobServer().getPollIntervalInSeconds() != null) {
            backgroundJobServerConfiguration.andPollIntervalInSeconds(properties.getBackgroundJobServer().getPollIntervalInSeconds());
        }

        if (properties.getBackgroundJobServer().getDeleteSucceededJobsAfter() != null) {
            backgroundJobServerConfiguration.andDeleteSucceededJobsAfter(properties.getBackgroundJobServer().getDeleteSucceededJobsAfter());
        }

        if (properties.getBackgroundJobServer().getPermanentlyDeleteDeletedJobsAfter() != null) {
            backgroundJobServerConfiguration.andPermanentlyDeleteDeletedJobsAfter(properties.getBackgroundJobServer().getPermanentlyDeleteDeletedJobsAfter());
        }

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

}
