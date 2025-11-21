package org.jobrunr.spring.autoconfigure;

import org.jobrunr.jobs.details.CachingJobDetailsGenerator;
import org.jobrunr.jobs.filters.RetryFilter;
import org.jobrunr.server.configuration.BackgroundJobServerThreadType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.jobrunr.server.BackgroundJobServerConfiguration.DEFAULT_PAGE_REQUEST_SIZE;

@ConfigurationProperties(prefix = "jobrunr")
public class JobRunrProperties {

    private Database database = new Database();
    private Jobs jobs = new Jobs();
    private JobScheduler jobScheduler = new JobScheduler();
    private Dashboard dashboard = new Dashboard();
    private BackgroundJobServer backgroundJobServer = new BackgroundJobServer();
    private Miscellaneous miscellaneous = new Miscellaneous();

    public Database getDatabase() {
        return database;
    }

    public void setDatabase(Database database) {
        this.database = database;
    }

    public Jobs getJobs() {
        return jobs;
    }

    public void setJobs(Jobs jobs) {
        this.jobs = jobs;
    }

    public JobScheduler getJobScheduler() {
        return jobScheduler;
    }

    public void setJobScheduler(JobScheduler jobScheduler) {
        this.jobScheduler = jobScheduler;
    }

    public Dashboard getDashboard() {
        return dashboard;
    }

    public void setDashboard(Dashboard dashboard) {
        this.dashboard = dashboard;
    }

    public BackgroundJobServer getBackgroundJobServer() {
        return backgroundJobServer;
    }

    public void setBackgroundJobServer(BackgroundJobServer backgroundJobServer) {
        this.backgroundJobServer = backgroundJobServer;
    }

    public Miscellaneous getMiscellaneous() {
        return miscellaneous;
    }

    public void setMiscellaneous(Miscellaneous miscellaneous) {
        this.miscellaneous = miscellaneous;
    }

    /**
     * JobRunr database related settings. These settings may not have an effect for certain NoSQL Databases (e.g. MongoDB).
     */
    public static class Database {
        /**
         * Allows to skip the creation of the tables - this means you should add them manually or by database migration tools like FlywayDB.
         */
        private boolean skipCreate = false;

        /**
         * The name of the database to use (only used by MongoDBStorageProvider). By default, it is 'jobrunr'.
         */
        private String databaseName;

        /**
         * Allows to set the table prefix used by JobRunr
         */
        private String tablePrefix;

        /**
         * An optional named {@link javax.sql.DataSource} to use. Defaults to the 'default' datasource.
         */
        private String datasource;

        /**
         * If multiple types of databases are available in the Spring Context (e.g. a DataSource and an MongoDB Client), this setting allows to specify the type of database for JobRunr to use.
         * Valid values are 'sql', 'mongodb' and 'mem'.
         */
        private String type;

        public void setSkipCreate(boolean skipCreate) {
            this.skipCreate = skipCreate;
        }

        public boolean isSkipCreate() {
            return skipCreate;
        }

        public String getTablePrefix() {
            return tablePrefix;
        }

        public void setTablePrefix(String tablePrefix) {
            this.tablePrefix = tablePrefix;
        }

        public String getDatabaseName() {
            return databaseName;
        }

        public void setDatabaseName(String databaseName) {
            this.databaseName = databaseName;
        }

        public String getDatasource() {
            return datasource;
        }

        public void setDatasource(String datasource) {
            this.datasource = datasource;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    /**
     * JobRunr job related settings
     */
    public static class Jobs {

        /**
         * Configures the default amount of retries.
         */
        private int defaultNumberOfRetries = RetryFilter.DEFAULT_NBR_OF_RETRIES;

        /**
         * Configures the seed for the exponential back-off when jobs are retried in case of an Exception.
         */
        private int backOffTimeSeed = RetryFilter.DEFAULT_BACKOFF_POLICY_TIME_SEED;

        /**
         * Configures MicroMeter metrics related to jobs
         */
        private Metrics metrics = new Metrics();

        public int getDefaultNumberOfRetries() {
            return defaultNumberOfRetries;
        }

        public void setDefaultNumberOfRetries(int defaultNumberOfRetries) {
            this.defaultNumberOfRetries = defaultNumberOfRetries;
        }

        public int getRetryBackOffTimeSeed() {
            return backOffTimeSeed;
        }

        public void setRetryBackOffTimeSeed(int backOffTimeSeed) {
            this.backOffTimeSeed = backOffTimeSeed;
        }

        public Metrics getMetrics() {
            return metrics;
        }

        public void setMetrics(Metrics metrics) {
            this.metrics = metrics;
        }

    }

    /**
     * JobRunr JobScheduler related settings
     */
    public static class JobScheduler {

        /**
         * Enables the scheduling of jobs.
         */
        private boolean enabled = true;

        /**
         * Defines the JobDetailsGenerator to use. This should be the fully qualified classname of the
         * JobDetailsGenerator, and it should have a default no-argument constructor.
         */
        private String jobDetailsGenerator = CachingJobDetailsGenerator.class.getName();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getJobDetailsGenerator() {
            return jobDetailsGenerator;
        }

        public void setJobDetailsGenerator(String jobDetailsGenerator) {
            this.jobDetailsGenerator = jobDetailsGenerator;
        }
    }

    /**
     * JobRunr BackgroundJobServer related settings
     */
    public static class BackgroundJobServer {

        /**
         * Allows to set the name of the {@link org.jobrunr.server.BackgroundJobServer} (used in the dashboard).
         */
        private String name;

        /**
         * Enables the background processing of jobs.
         */
        private boolean enabled = false;

        /**
         * Sets the workerCount for the BackgroundJobServer which defines the maximum number of jobs that will be run in parallel.
         * By default, this will be determined by the amount of available processor.
         */
        private Integer workerCount;

        /**
         * Sets the Thread Type for the BackgroundJobServer.
         * By default, this will be determined by the Java version (VirtualThreads as of Java 21).
         */
        private BackgroundJobServerThreadType threadType;

        /**
         * Set the pollIntervalInSeconds for the BackgroundJobServer to see whether new jobs need to be processed
         */
        private Integer pollIntervalInSeconds = 15;
        /**
         * Sets the maximum number of carbon aware jobs to update from awaiting to scheduled state per database round-trip.
         */
        private Integer carbonAwaitingJobsRequestSize = DEFAULT_PAGE_REQUEST_SIZE;

        /**
         * Set the pollInterval multiplicand used to determine when a BackgroundJobServer has timed out and processing jobs are orphaned.
         */
        private Integer serverTimeoutPollIntervalMultiplicand = 4;

        /**
         * Sets the maximum number of jobs to update from scheduled to enqueued state per database round-trip.
         */
        private Integer scheduledJobsRequestSize = DEFAULT_PAGE_REQUEST_SIZE;

        /**
         * Sets the maximum number of orphaned jobs to fetch and update per database round-trip (to retry them).
         */
        private Integer orphanedJobsRequestSize = DEFAULT_PAGE_REQUEST_SIZE;

        /**
         * Sets the maximum number of jobs to update from succeeded to deleted state per database round-trip.
         */
        private Integer succeededJobsRequestSize = DEFAULT_PAGE_REQUEST_SIZE;

        /**
         * Sets the duration to wait before changing jobs that are in the SUCCEEDED state to the DELETED state. If a duration suffix
         * is not specified, hours will be used.
         */
        @DurationUnit(HOURS)
        private Duration deleteSucceededJobsAfter = Duration.ofHours(36);

        /**
         * Sets the duration to wait before permanently deleting jobs that are in the DELETED state. If a duration suffix
         * is not specified, hours will be used.
         */
        @DurationUnit(HOURS)
        private Duration permanentlyDeleteDeletedJobsAfter = Duration.ofHours(72);

        /**
         * Sets the duration to wait before interrupting threads/jobs when the server is stopped. If a duration suffix
         * is not specified, seconds will be used.
         */
        @DurationUnit(SECONDS)
        private Duration interruptJobsAwaitDurationOnStop = Duration.ofSeconds(10);

        /**
         * Configures carbon-aware job processing properties
         */
        private CarbonAwareJobProcessing carbonAwareJobProcessing = new CarbonAwareJobProcessing();

        /**
         * Configures MicroMeter metrics related to the BackgroundJobServer
         */
        private Metrics metrics = new Metrics();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Integer getWorkerCount() {
            return workerCount;
        }

        public void setWorkerCount(Integer workerCount) {
            this.workerCount = workerCount;
        }

        public BackgroundJobServerThreadType getThreadType() {
            return threadType;
        }

        public void setThreadType(BackgroundJobServerThreadType threadType) {
            this.threadType = threadType;
        }

        public Integer getPollIntervalInSeconds() {
            return pollIntervalInSeconds;
        }

        public void setPollIntervalInSeconds(Integer pollIntervalInSeconds) {
            this.pollIntervalInSeconds = pollIntervalInSeconds;
        }

        public Integer getCarbonAwaitingJobsRequestSize() {
            return carbonAwaitingJobsRequestSize;
        }

        public void setCarbonAwaitingJobsRequestSize(Integer carbonAwaitingJobsRequestSize) {
            this.carbonAwaitingJobsRequestSize = carbonAwaitingJobsRequestSize;
        }

        public Integer getServerTimeoutPollIntervalMultiplicand() {
            return serverTimeoutPollIntervalMultiplicand;
        }

        public void setServerTimeoutPollIntervalMultiplicand(Integer serverTimeoutPollIntervalMultiplicand) {
            this.serverTimeoutPollIntervalMultiplicand = serverTimeoutPollIntervalMultiplicand;
        }

        public Integer getScheduledJobsRequestSize() {
            return scheduledJobsRequestSize;
        }

        public BackgroundJobServer setScheduledJobsRequestSize(Integer scheduledJobsRequestSize) {
            this.scheduledJobsRequestSize = scheduledJobsRequestSize;
            return this;
        }

        public Integer getOrphanedJobsRequestSize() {
            return orphanedJobsRequestSize;
        }

        public BackgroundJobServer setOrphanedJobsRequestSize(Integer orphanedJobsRequestSize) {
            this.orphanedJobsRequestSize = orphanedJobsRequestSize;
            return this;
        }

        public Integer getSucceededJobsRequestSize() {
            return succeededJobsRequestSize;
        }

        public BackgroundJobServer setSucceededJobsRequestSize(Integer succeededJobsRequestSize) {
            this.succeededJobsRequestSize = succeededJobsRequestSize;
            return this;
        }

        public Duration getDeleteSucceededJobsAfter() {
            return deleteSucceededJobsAfter;
        }

        public void setDeleteSucceededJobsAfter(Duration deleteSucceededJobsAfter) {
            this.deleteSucceededJobsAfter = deleteSucceededJobsAfter;
        }

        public Duration getPermanentlyDeleteDeletedJobsAfter() {
            return permanentlyDeleteDeletedJobsAfter;
        }

        public void setPermanentlyDeleteDeletedJobsAfter(Duration permanentlyDeleteDeletedJobsAfter) {
            this.permanentlyDeleteDeletedJobsAfter = permanentlyDeleteDeletedJobsAfter;
        }

        public Duration getInterruptJobsAwaitDurationOnStop() {
            return interruptJobsAwaitDurationOnStop;
        }

        public void setInterruptJobsAwaitDurationOnStop(Duration interruptJobsAwaitDurationOnStop) {
            this.interruptJobsAwaitDurationOnStop = interruptJobsAwaitDurationOnStop;
        }

        public CarbonAwareJobProcessing getCarbonAwareJobProcessing() {
            return carbonAwareJobProcessing;
        }

        public void setCarbonAwareJobProcessing(CarbonAwareJobProcessing carbonAwareJobProcessing) {
            this.carbonAwareJobProcessing = carbonAwareJobProcessing;
        }


        public Metrics getMetrics() {
            return metrics;
        }

        public void setMetrics(Metrics metrics) {
            this.metrics = metrics;
        }
    }

    /**
     * JobRunr dashboard related settings
     */
    public static class Dashboard {

        /**
         * Enables the JobRunr dashboard.
         */
        private boolean enabled = false;

        /**
         * The port on which the Dashboard should run
         */
        private int port = 8000;

        /**
         * The username for the basic authentication which protects the dashboard
         */
        private String username = null;

        /**
         * The password for the basic authentication which protects the dashboard. WARNING: this is insecure as it is in clear text
         */
        private String password = null;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }


        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }

    /**
     * Miscellaneous settings
     */
    public static class Miscellaneous {
        /**
         * Allows to opt-out of anonymous usage statistics. This setting is true by default and sends only the total amount of succeeded jobs processed
         * by your cluster per day to show a counter on the JobRunr website for marketing purposes.
         */
        private boolean allowAnonymousDataUsage = true;

        public boolean isAllowAnonymousDataUsage() {
            return allowAnonymousDataUsage;
        }

        public void setAllowAnonymousDataUsage(boolean allowAnonymousDataUsage) {
            this.allowAnonymousDataUsage = allowAnonymousDataUsage;
        }
    }

    /**
     * JobRunr MicroMeter Metrics related settings
     */
    public static class Metrics {

        /**
         * Configures whether metrics are reported to MicroMeter.
         */
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class CarbonAwareJobProcessing {
        /**
         * Enables carbon aware scheduling.
         */
        boolean enabled = false;

        /**
         * Sets your preferred carbon intensity forecast dataProvider.
         */
        String dataProvider;

        /**
         * Sets the areaCode of your datacenter (the area where your application is hosted) in order to have more accurate carbon emissions forecasts.
         * Cannot be used together with externalCode or externalIdentifier.
         */
        String areaCode;

        /**
         * Sets the code of an area as defined by your specified dataProvider in order to have more accurate carbon emissions forecasts.
         * Cannot be used together with areaCode or externalIdentifier and without dataProvider.
         */
        String externalCode;

        /**
         * Sets the identifier of an area as defined by your specified dataProvider in order to have more accurate carbon emissions forecasts.
         * Cannot be used together with areaCode or externalCode and without dataProvider.
         */
        String externalIdentifier;

        /**
         * Sets the connect timeout for the API client.
         */
        @DurationUnit(MILLIS)
        Duration apiClientConnectTimeout;

        /**
         * Sets the read timeout for the API client.
         */
        @DurationUnit(MILLIS)
        Duration apiClientReadTimeout;

        /**
         * Set the carbonAwareJobProcessingPollIntervalInMinutes for the BackgroundJobServer to see whether pending carbon aware jobs need to be scheduled
         */
        Integer pollIntervalInMinutes = 5;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getAreaCode() {
            return areaCode;
        }

        public void setAreaCode(String areaCode) {
            this.areaCode = areaCode;
        }

        public String getDataProvider() {
            return dataProvider;
        }

        public void setDataProvider(String dataProvider) {
            this.dataProvider = dataProvider;
        }

        public String getExternalCode() {
            return externalCode;
        }

        public void setExternalCode(String externalCode) {
            this.externalCode = externalCode;
        }

        public Duration getApiClientConnectTimeout() {
            return apiClientConnectTimeout;
        }

        public void setApiClientConnectTimeout(Duration apiClientConnectTimeout) {
            this.apiClientConnectTimeout = apiClientConnectTimeout;
        }

        public String getExternalIdentifier() {
            return externalIdentifier;
        }

        public void setExternalIdentifier(String externalIdentifier) {
            this.externalIdentifier = externalIdentifier;
        }

        public Duration getApiClientReadTimeout() {
            return apiClientReadTimeout;
        }

        public void setApiClientReadTimeout(Duration apiClientReadTimeout) {
            this.apiClientReadTimeout = apiClientReadTimeout;
        }

        public Integer getPollIntervalInMinutes() {
            return pollIntervalInMinutes;
        }

        public void setPollIntervalInMinutes(Integer pollIntervalInMinutes) {
            this.pollIntervalInMinutes = pollIntervalInMinutes;
        }

    }
}
