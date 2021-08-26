package org.jobrunr.configuration;

import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.jobrunr.dashboard.JobRunrDashboardWebServerConfiguration;
import org.jobrunr.jobs.details.CachingJobDetailsGenerator;
import org.jobrunr.jobs.details.JobDetailsAsmGenerator;
import org.jobrunr.jobs.details.JobDetailsGenerator;
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

import static org.jobrunr.dashboard.JobRunrDashboardWebServerConfiguration.usingStandardDashboardConfiguration;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.jobrunr.utils.reflection.ReflectionUtils.classExists;

/**
 * The main class to configure JobRunr
 */
public class JobRunrConfiguration {

    JobActivator jobActivator;
    final JsonMapper jsonMapper;
    final JobMapper jobMapper;
    final List<JobFilter> jobFilters;
    JobDetailsGenerator jobDetailsGenerator;
    StorageProvider storageProvider;
    BackgroundJobServer backgroundJobServer;
    JobRunrDashboardWebServer dashboardWebServer;
    JobRunrJMXExtensions jmxExtension;

    JobRunrConfiguration() {
        this.jsonMapper = determineJsonMapper();
        this.jobMapper = new JobMapper(jsonMapper);
        this.jobDetailsGenerator = new CachingJobDetailsGenerator();
        this.jobFilters = new ArrayList<>();
    }

    /**
     * The {@link JobActivator} is used to resolve jobs from the IoC framework
     *
     * @param jobActivator the {@link JobActivator} to use
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrConfiguration useJobActivator(JobActivator jobActivator) {
        if (this.backgroundJobServer != null) {
            throw new IllegalStateException("Please configure the JobActivator before the BackgroundJobServer.");
        }
        this.jobActivator = jobActivator;
        return this;
    }

    /**
     * Allows to set the StorageProvider that JobRunr will use.
     *
     * @param storageProvider the StorageProvider to use
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrConfiguration useStorageProvider(StorageProvider storageProvider) {
        this.storageProvider = storageProvider;
        storageProvider.setJobMapper(jobMapper);
        return this;
    }

    /**
     * Allows to set extra JobFilters or to provide another implementation of the {@link org.jobrunr.jobs.filters.RetryFilter}
     *
     * @param jobFilters the jobFilters to use for each job.
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrConfiguration withJobFilter(JobFilter... jobFilters) {
        if (this.backgroundJobServer != null) {
            throw new IllegalStateException("Please configure the JobFilters before the BackgroundJobServer.");
        }
        this.jobFilters.addAll(Arrays.asList(jobFilters));
        return this;
    }

    /**
     * Provides a default {@link BackgroundJobServer} that is configured using a number of threads depending on the amount of CPU.
     *
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrConfiguration useBackgroundJobServer() {
        return useBackgroundJobServerIf(true);
    }

    /**
     * Provides a default {@link BackgroundJobServer} if the guard is true and that is configured using a number of threads depending on the amount of CPU.
     *
     * @param guard whether to start a BackgroundJobServer or not.
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrConfiguration useBackgroundJobServerIf(boolean guard) {
        return useBackgroundJobServerIf(guard, usingStandardBackgroundJobServerConfiguration());
    }

    /**
     * Provides a default {@link BackgroundJobServer} that is configured using a given number of threads.
     *
     * @param workerCount the number of worker threads to use
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrConfiguration useBackgroundJobServer(int workerCount) {
        return useBackgroundJobServerIf(true, workerCount);
    }

    /**
     * Provides a default {@link BackgroundJobServer} if the guard is true and that is configured using a given number of threads.
     *
     * @param guard       whether to start a BackgroundJobServer or not.
     * @param workerCount the number of worker threads to use
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrConfiguration useBackgroundJobServerIf(boolean guard, int workerCount) {
        return useBackgroundJobServerIf(guard, usingStandardBackgroundJobServerConfiguration().andWorkerCount(workerCount));
    }

    /**
     * Provides a default {@link BackgroundJobServer} that is configured using the given {@link BackgroundJobServerConfiguration}
     *
     * @param configuration the configuration for the backgroundJobServer to use
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrConfiguration useBackgroundJobServer(BackgroundJobServerConfiguration configuration) {
        return useBackgroundJobServerIf(true, configuration);
    }

    /**
     * Provides a default {@link BackgroundJobServer} that is configured using the given {@link BackgroundJobServerConfiguration}
     *
     * @param configuration            the configuration for the backgroundJobServer to use
     * @param startBackgroundJobServer whether to start the background job server immediately
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrConfiguration useBackgroundJobServer(BackgroundJobServerConfiguration configuration, boolean startBackgroundJobServer) {
        return useBackgroundJobServerIf(true, configuration, startBackgroundJobServer);
    }

    /**
     * Provides a default {@link BackgroundJobServer} if the guard is true and that is configured using the given {@link BackgroundJobServerConfiguration}
     *
     * @param guard         whether to start a BackgroundJobServer or not.
     * @param configuration the configuration for the backgroundJobServer to use
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrConfiguration useBackgroundJobServerIf(boolean guard, BackgroundJobServerConfiguration configuration) {
        return useBackgroundJobServerIf(guard, configuration, true);
    }

    /**
     * Provides a default {@link BackgroundJobServer} if the guard is true and that is configured using the given {@link BackgroundJobServerConfiguration}
     *
     * @param guard                    whether to create a BackgroundJobServer or not.
     * @param configuration            the configuration for the backgroundJobServer to use
     * @param startBackgroundJobServer whether to start the background job server immediately
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrConfiguration useBackgroundJobServerIf(boolean guard, BackgroundJobServerConfiguration configuration, boolean startBackgroundJobServer) {
        if (guard) {
            this.backgroundJobServer = new BackgroundJobServer(storageProvider, jsonMapper, jobActivator, configuration);
            this.backgroundJobServer.setJobFilters(jobFilters);
            this.backgroundJobServer.start(startBackgroundJobServer);
        }
        return this;
    }

    /**
     * Provides a dashboard on port 8000
     *
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrConfiguration useDashboard() {
        return useDashboardIf(true);
    }

    /**
     * Provides a dashboard on port 8000 if the guard is true
     *
     * @param guard whether to start a Dashboard or not.
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrConfiguration useDashboardIf(boolean guard) {
        return useDashboardIf(guard, usingStandardDashboardConfiguration());
    }

    /**
     * Provides a dashboard on the given port
     *
     * @param dashboardPort the port on which to start the {@link JobRunrDashboardWebServer}
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrConfiguration useDashboard(int dashboardPort) {
        return useDashboardIf(true, dashboardPort);
    }

    /**
     * Provides a dashboard on the given port if the guard is true
     *
     * @param guard         whether to start a Dashboard or not.
     * @param dashboardPort the port on which to start the {@link JobRunrDashboardWebServer}
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrConfiguration useDashboardIf(boolean guard, int dashboardPort) {
        return useDashboardIf(guard, usingStandardDashboardConfiguration().andPort(dashboardPort));
    }

    /**
     * Provides a dashboard using the given {@link JobRunrDashboardWebServerConfiguration}
     *
     * @param configuration the {@link JobRunrDashboardWebServerConfiguration} to use
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrConfiguration useDashboard(JobRunrDashboardWebServerConfiguration configuration) {
        return useDashboardIf(true, configuration);
    }

    /**
     * Provides a dashboard using the given {@link JobRunrDashboardWebServerConfiguration} if the guard is true
     *
     * @param guard         whether to start a Dashboard or not.
     * @param configuration the {@link JobRunrDashboardWebServerConfiguration} to use
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrConfiguration useDashboardIf(boolean guard, JobRunrDashboardWebServerConfiguration configuration) {
        if (guard) {
            this.dashboardWebServer = new JobRunrDashboardWebServer(storageProvider, jsonMapper, configuration);
            this.dashboardWebServer.start();
        }
        return this;
    }

    /**
     * If called, this method will register JMX Extensions to monitor JobRunr via JMX
     *
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrConfiguration useJmxExtensions() {
        return useJmxExtensionsIf(true);
    }

    /**
     * Enables JMX Extensions to monitor JobRunr via JMX if the guard is true
     *
     * @param guard whether to start the JXM Extensions or not.
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrConfiguration useJmxExtensionsIf(boolean guard) {
        if (guard) {
            if (backgroundJobServer == null)
                throw new IllegalStateException("Please configure the BackgroundJobServer before the JMXExtension.");
            if (storageProvider == null)
                throw new IllegalStateException("Please configure the StorageProvider before the JMXExtension.");
            this.jmxExtension = new JobRunrJMXExtensions(backgroundJobServer, storageProvider);
        }
        return this;
    }

    /**
     * Specifies which {@link JobDetailsGenerator} to use.
     *
     * @param jobDetailsGenerator the JobDetailsGenerator to use.
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrConfiguration useJobDetailsGenerator(JobDetailsGenerator jobDetailsGenerator) {
        this.jobDetailsGenerator = jobDetailsGenerator;
        return this;
    }

    /**
     * Initializes JobRunr and returns a {@link JobScheduler} which can then be used to register in the IoC framework
     * or to enqueue/schedule some Jobs.
     *
     * @return a JobScheduler to enqueue/schedule new jobs
     */
    public JobScheduler initialize() {
        final JobScheduler jobScheduler = new JobScheduler(storageProvider, jobDetailsGenerator, jobFilters);
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
