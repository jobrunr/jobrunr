package org.jobrunr.configuration;

import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.jobrunr.dashboard.JobRunrDashboardWebServerConfiguration;
import org.jobrunr.jobs.details.CachingJobDetailsGenerator;
import org.jobrunr.jobs.details.JobDetailsGenerator;
import org.jobrunr.jobs.filters.JobFilter;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.server.JobActivator;
import org.jobrunr.server.costaware.CostAwareConfiguration;
import org.jobrunr.server.jmx.JobRunrJMXExtensions;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.JsonMapperException;
import org.jobrunr.utils.mapper.JsonMapperFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Optional.ofNullable;
import static org.jobrunr.dashboard.JobRunrDashboardWebServerConfiguration.usingStandardDashboardConfiguration;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.jobrunr.utils.mapper.JsonMapperValidator.validateJsonMapper;

/**
 * The main class to configure JobRunr
 */
public class JobRunrConfiguration {

    JobActivator jobActivator;
    JsonMapper jsonMapper;
    JobMapper jobMapper;
    final List<JobFilter> jobFilters;
    JobDetailsGenerator jobDetailsGenerator;
    StorageProvider storageProvider;
    BackgroundJobServer backgroundJobServer;
    JobRunrDashboardWebServer dashboardWebServer;
    JobRunrJMXExtensions jmxExtension;
    JobRunrMicroMeterIntegration microMeterIntegration;
    // TODO this belongs to BackgroundJobServerConfiguration
    CostAwareConfiguration costAwareConfiguration;

    private BackgroundJobServerConfiguration backgroundJobServerConfiguration;
    private boolean startBackgroundJobServerOnInitialization;
    private JobRunrDashboardWebServerConfiguration dashboardWebServerConfiguration;
    private boolean startDashboardWebServerOnInitialization;
    private boolean jmxExtensionEnabled;
    private boolean jmxExtensionJobStatisticsEnabled;

    JobRunrConfiguration() {
        this.jsonMapper = JsonMapperFactory.createJsonMapper();
        this.jobMapper = this.jsonMapper == null ? null : new JobMapper(jsonMapper);
        this.jobDetailsGenerator = new CachingJobDetailsGenerator();
        this.jobFilters = new ArrayList<>();
    }

    /**
     * The {@link JsonMapper} to transform jobs to json in the database
     *
     * @param jsonMapper the {@link JsonMapper} to use
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrConfiguration useJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = validateJsonMapper(jsonMapper);
        this.jobMapper = new JobMapper(jsonMapper);
        return this;
    }

    /**
     * The {@link JobActivator} is used to resolve jobs from the IoC framework
     *
     * @param jobActivator the {@link JobActivator} to use
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrConfiguration useJobActivator(JobActivator jobActivator) {
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
        return this;
    }

    /**
     * Allows setting extra JobFilters or to provide another implementation of the {@link org.jobrunr.jobs.filters.RetryFilter}
     *
     * @param jobFilters the jobFilters to use for each job.
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrConfiguration withJobFilter(JobFilter... jobFilters) {
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
     * @param guard                                    whether to create a BackgroundJobServer or not.
     * @param configuration                            the configuration for the backgroundJobServer to use
     * @param startBackgroundJobServerOnInitialization whether to start the background job server immediately on initialization
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrConfiguration useBackgroundJobServerIf(boolean guard, BackgroundJobServerConfiguration configuration, boolean startBackgroundJobServerOnInitialization) {
        if (guard) {
            this.backgroundJobServerConfiguration = configuration;
            this.startBackgroundJobServerOnInitialization = startBackgroundJobServerOnInitialization;
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
        return useDashboard(configuration, true);
    }

    /**
     * Provides a dashboard using the given {@link JobRunrDashboardWebServerConfiguration}
     *
     * @param configuration                           the {@link JobRunrDashboardWebServerConfiguration} to use
     * @param startDashboardWebServerOnInitialization whether to start the dashboard web server immediately on initialization
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrConfiguration useDashboard(JobRunrDashboardWebServerConfiguration configuration, boolean startDashboardWebServerOnInitialization) {
        return useDashboardIf(true, configuration, startDashboardWebServerOnInitialization);
    }

    /**
     * Provides a dashboard using the given {@link JobRunrDashboardWebServerConfiguration} if the guard is true
     *
     * @param guard         whether to start a Dashboard or not.
     * @param configuration the {@link JobRunrDashboardWebServerConfiguration} to use
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrConfiguration useDashboardIf(boolean guard, JobRunrDashboardWebServerConfiguration configuration) {
        return useDashboardIf(guard, configuration, true);
    }

    /**
     * Provides a dashboard using the given {@link JobRunrDashboardWebServerConfiguration} if the guard is true
     *
     * @param guard                                   whether to create a Dashboard or not.
     * @param configuration                           the {@link JobRunrDashboardWebServerConfiguration} to use
     * @param startDashboardWebServerOnInitialization whether to start the dashboard web server immediately on initialization
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrConfiguration useDashboardIf(boolean guard, JobRunrDashboardWebServerConfiguration configuration, boolean startDashboardWebServerOnInitialization) {
        if (guard) {
            this.dashboardWebServerConfiguration = configuration;
            this.startDashboardWebServerOnInitialization = startDashboardWebServerOnInitialization;
        }
        return this;
    }

    public JobRunrConfiguration useCostAware(CostAwareConfiguration costAwareConfiguration) {
        if (this.backgroundJobServer != null) {
            throw new IllegalStateException("Please configure CostAware before the BackgroundJobServer.");
        }
        this.costAwareConfiguration = costAwareConfiguration;
        return this;
    }

    public JobRunrConfiguration useCostAwareIf(boolean guard, CostAwareConfiguration costAwareConfiguration) {
        if (guard) {
            if (this.backgroundJobServer != null) {
                throw new IllegalStateException("Please configure CostAware before the BackgroundJobServer.");
            }
            this.costAwareConfiguration = costAwareConfiguration;
        }
        return this;
    }

    /**
     * If called, this method will register JMX Extensions to monitor JobRunr via JMX
     *
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrConfiguration useJmxExtensions() {
        return useJmxExtensionsIf(true, true);
    }

    /**
     * If called, this method will register JMX Extensions to monitor JobRunr via JMX
     *
     * @param reportJobStatistics allows to enable or disable reporting of job statistics (note: there is a performance hit on your {@link StorageProvider} by enabling it)
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrConfiguration useJmxExtensions(boolean reportJobStatistics) {
        return useJmxExtensionsIf(true, reportJobStatistics);
    }

    /**
     * Enables JMX Extensions to monitor JobRunr via JMX if the guard is true
     *
     * @param guard               whether to start the JXM Extensions or not.
     * @param reportJobStatistics allows to enable or disable reporting of job statistics (note: there is a performance hit on your {@link StorageProvider} by enabling it)
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrConfiguration useJmxExtensionsIf(boolean guard, boolean reportJobStatistics) {
        this.jmxExtensionEnabled = guard;
        this.jmxExtensionJobStatisticsEnabled = reportJobStatistics;
        return this;
    }

    /**
     * Allows integrating MicroMeter metrics into JobRunr.
     *
     * @param microMeterIntegration the JobRunrMicroMeterIntegration
     * @return the same configuration instance which provides a fluent api
     * @deprecated please use {@link JobRunrConfiguration#useMetrics(JobRunrMicroMeterIntegration)} instead.
     */
    @Deprecated
    public JobRunrConfiguration useMicroMeter(JobRunrMicroMeterIntegration microMeterIntegration) {
        this.microMeterIntegration = microMeterIntegration;
        return this;
    }

    /**
     * Allows integrating MicroMeter metrics into JobRunr.
     *
     * @param microMeterIntegration the JobRunrMicroMeterIntegration
     * @return the same configuration instance which provides a fluent api
     */
    public JobRunrConfiguration useMetrics(JobRunrMicroMeterIntegration microMeterIntegration) {
        this.microMeterIntegration = microMeterIntegration;
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
    public JobRunrConfigurationResult initialize() {
        if (jsonMapper == null) {
            throw new JsonMapperException("No JsonMapper class is found. Make sure you have either Jackson, Gson or a JsonB compliant library available on your classpath. You may also configure a custom JsonMapper.");
        }

        if (jobMapper != null && storageProvider != null) storageProvider.setJobMapper(jobMapper);

        initializeBackgroundJobServer();
        initializeDashboardWebServer();
        initializeJmxExtensions();

        ofNullable(microMeterIntegration).ifPresent(meterRegistry -> meterRegistry.initialize(storageProvider, backgroundJobServer));
        final JobScheduler jobScheduler = new JobScheduler(storageProvider, jobDetailsGenerator, jobFilters);
        final JobRequestScheduler jobRequestScheduler = new JobRequestScheduler(storageProvider, jobFilters);
        return new JobRunrConfigurationResult(jobScheduler, jobRequestScheduler, dashboardWebServer, backgroundJobServer);
    }

    private void initializeBackgroundJobServer() {
        if (backgroundJobServerConfiguration != null) {
            this.backgroundJobServer = new BackgroundJobServer(storageProvider, jsonMapper, jobActivator, backgroundJobServerConfiguration, costAwareConfiguration);
            this.backgroundJobServer.setJobFilters(jobFilters);
            if (startBackgroundJobServerOnInitialization) this.backgroundJobServer.start();
        }
    }

    private void initializeDashboardWebServer() {
        if (dashboardWebServerConfiguration != null) {
            this.dashboardWebServer = new JobRunrDashboardWebServer(storageProvider, jsonMapper, dashboardWebServerConfiguration);
            if (startDashboardWebServerOnInitialization) this.dashboardWebServer.start();
        }
    }

    private void initializeJmxExtensions() {
        if (jmxExtensionEnabled) {
            if (backgroundJobServer == null)
                throw new IllegalStateException("Please configure the BackgroundJobServer before the JMXExtension.");
            if (storageProvider == null)
                throw new IllegalStateException("Please configure the StorageProvider before the JMXExtension.");
            this.jmxExtension = new JobRunrJMXExtensions(backgroundJobServer, storageProvider, jmxExtensionJobStatisticsEnabled);
        }
    }

    public static class JobRunrConfigurationResult {

        private final JobScheduler jobScheduler;
        private final JobRequestScheduler jobRequestScheduler;
        private final JobRunrDashboardWebServer dashboardWebServer;
        private final BackgroundJobServer backgroundJobServer;

        public JobRunrConfigurationResult(JobScheduler jobScheduler, JobRequestScheduler jobRequestScheduler, JobRunrDashboardWebServer dashboardWebServer, BackgroundJobServer backgroundJobServer) {
            this.jobScheduler = jobScheduler;
            this.jobRequestScheduler = jobRequestScheduler;
            this.dashboardWebServer = dashboardWebServer;
            this.backgroundJobServer = backgroundJobServer;
        }

        public JobScheduler getJobScheduler() {
            return jobScheduler;
        }

        public JobRequestScheduler getJobRequestScheduler() {
            return jobRequestScheduler;
        }

        public JobRunrDashboardWebServer getDashboardWebServer() {
            return dashboardWebServer;
        }

        public BackgroundJobServer getBackgroundJobServer() {
            return backgroundJobServer;
        }
    }
}
