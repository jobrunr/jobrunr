package org.jobrunr.configuration;

import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.jobrunr.dashboard.JobRunrDashboardWebServerConfiguration;
import org.jobrunr.jobs.filters.JobFilter;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.server.JobActivator;
import org.jobrunr.server.jmx.JobRunrJMXExtensions;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.JsonMapperException;
import org.jobrunr.utils.mapper.gson.GsonJsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.jobrunr.utils.mapper.jsonb.JsonbJsonMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.jobrunr.utils.reflection.ReflectionUtils.classExists;

public class JobRunrConfiguration {

    StorageProvider storageProvider;
    JsonMapper jsonMapper;
    JobMapper jobMapper;
    BackgroundJobServer backgroundJobServer;
    JobRunrDashboardWebServer dashboardWebServer;
    JobActivator jobActivator;
    List<JobFilter> jobFilters;
    JobRunrJMXExtensions jmxExtension;

    JobRunrConfiguration() {
        this.jsonMapper = determineJsonMapper();
        this.jobMapper = new JobMapper(jsonMapper);
        this.jobFilters = new ArrayList<>();
    }

    public JobRunrConfiguration useStorageProvider(StorageProvider storageProvider) {
        this.storageProvider = storageProvider;
        storageProvider.setJobMapper(jobMapper);
        return this;
    }

    public JobRunrConfiguration withJobFilter(JobFilter... jobFilters) {
        this.jobFilters.addAll(Arrays.asList(jobFilters));
        return this;
    }

    public JobRunrConfiguration useDefaultBackgroundJobServer() {
        this.useBackgroundJobServer(new BackgroundJobServer(storageProvider, jobActivator));
        this.backgroundJobServer.start();
        this.backgroundJobServer.setJobFilters(jobFilters);
        return this;
    }

    public JobRunrConfiguration useDefaultBackgroundJobServer(int workerCount) {
        return useDefaultBackgroundJobServer(usingStandardBackgroundJobServerConfiguration().andWorkerCount(workerCount));
    }

    public JobRunrConfiguration useDefaultBackgroundJobServer(BackgroundJobServerConfiguration configuration) {
        this.useBackgroundJobServer(new BackgroundJobServer(storageProvider, jobActivator, configuration));
        this.backgroundJobServer.setJobFilters(jobFilters);
        this.backgroundJobServer.start();
        return this;
    }

    public JobRunrConfiguration useBackgroundJobServer(BackgroundJobServer backgroundJobServer) {
        this.backgroundJobServer = backgroundJobServer;
        this.backgroundJobServer.setJobFilters(jobFilters);
        return this;
    }

    public JobRunrConfiguration useDashboard() {
        this.dashboardWebServer = new JobRunrDashboardWebServer(storageProvider, jsonMapper);
        this.dashboardWebServer.start();
        return this;
    }

    public JobRunrConfiguration useDashboard(int dashboardPort) {
        this.dashboardWebServer = new JobRunrDashboardWebServer(storageProvider, jsonMapper, dashboardPort);
        this.dashboardWebServer.start();
        return this;
    }

    public JobRunrConfiguration useDashboard(JobRunrDashboardWebServerConfiguration configuration) {
        this.dashboardWebServer = new JobRunrDashboardWebServer(storageProvider, jsonMapper, configuration);
        this.dashboardWebServer.start();
        return this;
    }

    public JobRunrConfiguration useJobActivator(JobActivator jobActivator) {
        this.jobActivator = jobActivator;
        return this;
    }

    public JobRunrConfiguration useJmxExtensions() {
        if (backgroundJobServer == null)
            throw new IllegalStateException("Please configure the BackgroundJobServer before the JMXExtension.");
        if (storageProvider == null)
            throw new IllegalStateException("Please configure the StorageProvider before the JMXExtension.");
        this.jmxExtension = new JobRunrJMXExtensions(backgroundJobServer, storageProvider);
        return this;
    }

    public JobScheduler initialize() {
        final JobScheduler jobScheduler = new JobScheduler(storageProvider, jobFilters);
        BackgroundJob.setJobScheduler(jobScheduler);
        return jobScheduler;
    }

    private static JsonMapper determineJsonMapper() {
        if (classExists("com.fasterxml.jackson.databind.ObjectMapper")) {
            return new JacksonJsonMapper();
        } else if (classExists("com.google.gson.Gson")) {
            return new GsonJsonMapper();
        } else if (classExists("javax.json.bind.JsonbBuilder")) {
            return new JsonbJsonMapper();
        } else {
            throw new JsonMapperException("No JsonMapper class is found. Make sure you have either Jackson, Gson or a JsonB compliant library available on your classpath");
        }
    }
}
