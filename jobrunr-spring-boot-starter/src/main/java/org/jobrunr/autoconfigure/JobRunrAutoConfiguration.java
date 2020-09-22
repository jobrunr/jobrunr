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
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.gson.GsonJsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.time.Duration;

import static org.jobrunr.dashboard.JobRunrDashboardWebServerConfiguration.usingStandardDashboardConfiguration;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;

@Configuration
public class JobRunrAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "org.jobrunr", name = "job_scheduler", havingValue = "true", matchIfMissing = true)
    public JobScheduler jobScheduler(StorageProvider storageProvider) {
        final JobScheduler jobScheduler = new JobScheduler(storageProvider);
        BackgroundJob.setJobScheduler(jobScheduler);
        return jobScheduler;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "org.jobrunr", name = "background_job_server", havingValue = "true")
    public BackgroundJobServer backgroundJobServer(StorageProvider storageProvider, JobActivator jobActivator, BackgroundJobServerConfiguration backgroundJobServerConfiguration) {
        final BackgroundJobServer backgroundJobServer = new BackgroundJobServer(storageProvider, jobActivator, backgroundJobServerConfiguration);
        backgroundJobServer.start();
        return backgroundJobServer;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "org.jobrunr", name = "background_job_server", havingValue = "true")
    public BackgroundJobServerConfiguration backgroundJobServerConfiguration(Environment environment) {
        BackgroundJobServerConfiguration backgroundJobServerConfiguration = usingStandardBackgroundJobServerConfiguration();
        if (environment.containsProperty("org.jobrunr.background_job_server.worker_count")) {
            backgroundJobServerConfiguration.andWorkerCount(Integer.parseInt(environment.getRequiredProperty("org.jobrunr.background_job_server.worker_count")));
        }

        if (environment.containsProperty("org.jobrunr.background_job_server.poll_interval")) {
            backgroundJobServerConfiguration.andPollIntervalInSeconds(Integer.parseInt(environment.getRequiredProperty("org.jobrunr.background_job_server.poll_interval")));
        }

        if (environment.containsProperty("org.jobrunr.background_job_server.delete_succeeded_jobs_after")) {
            backgroundJobServerConfiguration.andDeleteSucceededJobsAfter(Duration.ofHours(Integer.parseInt(environment.getRequiredProperty("org.jobrunr.background_job_server.delete_succeeded_jobs_after"))));
        }

        if (environment.containsProperty("org.jobrunr.background_job_server.permanently_delete_deleted_jobs_after")) {
            backgroundJobServerConfiguration.andPermanentlyDeleteDeletedJobsAfter(Duration.ofHours(Integer.parseInt(environment.getRequiredProperty("org.jobrunr.background_job_server.permanently_delete_deleted_jobs_after"))));
        }

        return backgroundJobServerConfiguration;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "org.jobrunr", name = "dashboard", havingValue = "true")
    public JobRunrDashboardWebServer dashboardWebServer(StorageProvider storageProvider, JsonMapper jsonMapper, JobRunrDashboardWebServerConfiguration dashboardWebServerConfiguration) {
        JobRunrDashboardWebServer dashboardWebServer = new JobRunrDashboardWebServer(storageProvider, jsonMapper, dashboardWebServerConfiguration);
        dashboardWebServer.start();
        return dashboardWebServer;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "org.jobrunr", name = "dashboard", havingValue = "true")
    public JobRunrDashboardWebServerConfiguration dashboardWebServerConfiguration(Environment environment) {
        JobRunrDashboardWebServerConfiguration dashboardWebServerConfiguration = usingStandardDashboardConfiguration();
        if (environment.containsProperty("org.jobrunr.dashboard.port")) {
            dashboardWebServerConfiguration.andPort(Integer.parseInt(environment.getRequiredProperty("org.jobrunr.dashboard.port")));
        }
        return dashboardWebServerConfiguration;
    }

    @Bean
    @ConditionalOnMissingBean
    public JobActivator jobActivator(ApplicationContext applicationContext) {
        return applicationContext::getBean;
    }

    @Bean
    @ConditionalOnBean(name = "dataSource")
    @ConditionalOnMissingBean
    public StorageProvider storageProvider(DataSource dataSource, JobMapper jobMapper) {
        StorageProvider storageProvider = SqlStorageProviderFactory.using(dataSource);
        storageProvider.setJobMapper(jobMapper);
        return storageProvider;
    }

    @Bean
    @ConditionalOnMissingBean
    public JobMapper jobMapper(JsonMapper jsonMapper) {
        return new JobMapper(jsonMapper);
    }

    @Bean(name = "jsonMapper")
    @ConditionalOnMissingBean
    @ConditionalOnClass(ObjectMapper.class)
    public JsonMapper jacksonJsonMapper() {
        return new JacksonJsonMapper();
    }

    @Bean(name = "jsonMapper")
    @ConditionalOnMissingBean
    @ConditionalOnClass(Gson.class)
    public JsonMapper gsonJsonMapper() {
        return new GsonJsonMapper();
    }

}
