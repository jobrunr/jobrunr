package org.jobrunr.spring.autoconfigure;

import org.jobrunr.jobs.details.CachingJobDetailsGenerator;
import org.jobrunr.jobs.filters.RetryFilter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@ConfigurationProperties(prefix = "org.jobrunr")
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
     * JobRunr dashboard related settings. These settings may not have an effect for certain NoSQL Databases (e.g. Redis).
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
         * If multiple types of databases are available in the Spring Context (e.g. a DataSource and an Elastic RestHighLevelClient), this setting allows to specify the type of database for JobRunr to use.
         * Valid values are 'sql', 'mongodb', 'redis-lettuce', 'redis-jedis' and 'elasticsearch'.
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
         * Set the pollIntervalInSeconds for the BackgroundJobServer to see whether new jobs need to be processed
         */
        private Integer pollIntervalInSeconds = 15;

        /**
         * Sets the maximum number of jobs to update from scheduled to enqueued state per polling interval.
         */
        private Integer scheduledJobsRequestSize = 1000;

        /**
         * Sets the query size for misfired jobs per polling interval (to retry them).
         */
        private Integer orphanedJobsRequestSize = 1000;

        /**
         * Sets the maximum number of jobs to update from succeeded to deleted state per polling interval.
         */
        private Integer succeededJobsRequestSize = 1000;

        /**
         * Sets the duration to wait before changing jobs that are in the SUCCEEDED state to the DELETED state. If a duration suffix
         * is not specified, hours will be used.
         */
        @DurationUnit(ChronoUnit.HOURS)
        private Duration deleteSucceededJobsAfter = Duration.ofHours(36);

        /**
         * Sets the duration to wait before permanently deleting jobs that are in the DELETED state. If a duration suffix
         * is not specified, hours will be used.
         */
        @DurationUnit(ChronoUnit.HOURS)
        private Duration permanentlyDeleteDeletedJobsAfter = Duration.ofHours(72);

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

        public Integer getPollIntervalInSeconds() {
            return pollIntervalInSeconds;
        }

        public void setPollIntervalInSeconds(Integer pollIntervalInSeconds) {
            this.pollIntervalInSeconds = pollIntervalInSeconds;
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
}
