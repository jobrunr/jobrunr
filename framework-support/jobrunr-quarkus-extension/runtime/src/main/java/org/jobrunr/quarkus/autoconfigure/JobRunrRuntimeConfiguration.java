package org.jobrunr.quarkus.autoconfigure;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

import java.time.Duration;
import java.util.Optional;

@ConfigRoot(name = "jobrunr", phase = ConfigPhase.RUN_TIME)
public class JobRunrRuntimeConfiguration {

    public DatabaseConfiguration database;

    public JobsConfiguration jobs;

    public JobSchedulerConfiguration jobScheduler;

    public BackgroundJobServerConfiguration backgroundJobServer;

    public DashboardConfiguration dashboard;

    public MiscellaneousConfiguration miscellaneous;

    @ConfigGroup
    public static class DatabaseConfiguration {

        /**
         * Allows to skip the creation of the tables - this means you should add them manually or by database migration tools like FlywayDB or Liquibase.
         */
        @ConfigItem(defaultValue = "false")
        public boolean skipCreate;

        /**
         * Allows to set the database name to use (only used by MongoDBStorageProvider). By default, it is 'jobrunr'.
         */
        @ConfigItem
        public Optional<String> databaseName;

        /**
         * Allows to set the table prefix used by JobRunr
         */
        @ConfigItem
        public Optional<String> tablePrefix;

        /**
         * An optional named {@link javax.sql.DataSource} to use. Defaults to the 'default' datasource.
         */
        @ConfigItem
        public Optional<String> datasource;

        /**
         * If multiple types of databases are available in the Spring Context (e.g. a DataSource and an Elastic RestHighLevelClient), this setting allows to specify the type of database for JobRunr to use.
         * Valid values are 'sql', 'mongodb', 'documentdb', and 'elasticsearch'.
         */
        @ConfigItem
        public Optional<String> type;
    }

    @ConfigGroup
    public static class JobsConfiguration {

        /**
         * Configures the default amount of retries.
         */
        @ConfigItem
        public Optional<Integer> defaultNumberOfRetries;

        /**
         * Configures the seed for the exponential back-off when jobs are retried in case of an Exception.
         */
        @ConfigItem
        public Optional<Integer> retryBackOffTimeSeed;
    }

    @ConfigGroup
    public static class JobSchedulerConfiguration {
        /**
         * Defines the JobDetailsGenerator to use. This should be the fully qualified classname of the
         * JobDetailsGenerator, and it should have a default no-argument constructor.
         */
        @ConfigItem
        public Optional<String> jobDetailsGenerator;
    }

    @ConfigGroup
    public static class BackgroundJobServerConfiguration {

        /**
         * Sets the name of the {@link org.jobrunr.server.BackgroundJobServer} (used in the dashboard).
         */
        @ConfigItem
        public Optional<String> name;

        /**
         * Sets the workerCount for the BackgroundJobServer which defines the maximum number of jobs that will be run in parallel.
         * By default, this will be determined by the amount of available processor.
         */
        @ConfigItem
        public Optional<Integer> workerCount;

        /**
         * Sets the maximum number of jobs to update from scheduled to enqueued state per polling interval.
         */
        @ConfigItem
        public Optional<Integer> scheduledJobsRequestSize;

        /**
         * Sets the query size for misfired jobs per polling interval (to retry them).
         */
        @ConfigItem
        public Optional<Integer> orphanedJobsRequestSize;

        /**
         * Sets the maximum number of jobs to update from succeeded to deleted state per polling interval.
         */
        @ConfigItem
        public Optional<Integer> succeededsJobRequestSize;

        /**
         * Set the pollIntervalInSeconds for the BackgroundJobServer to see whether new jobs need to be processed
         */
        @ConfigItem
        public Optional<Integer> pollIntervalInSeconds;

        /**
         * Sets the duration to wait before changing jobs that are in the SUCCEEDED state to the DELETED state.
         */
        @ConfigItem
        public Optional<Duration> deleteSucceededJobsAfter;

        /**
         * Sets the duration to wait before permanently deleting jobs that are in the DELETED state.
         */
        @ConfigItem
        public Optional<Duration> permanentlyDeleteDeletedJobsAfter;
    }

    @ConfigGroup
    public static class DashboardConfiguration {

        /**
         * The port on which the Dashboard should run
         */
        @ConfigItem
        public Optional<Integer> port;

        /**
         * The username for the basic authentication which protects the dashboard
         */
        @ConfigItem
        public Optional<String> username;

        /**
         * The password for the basic authentication which protects the dashboard. WARNING: this is insecure as it is in clear text
         */
        @ConfigItem
        public Optional<String> password;
    }

    @ConfigGroup
    public static class MiscellaneousConfiguration {

        /**
         * Allows to opt-out of anonymous usage statistics. This setting is true by default and sends only the total amount of succeeded jobs processed
         * by your cluster per day to show a counter on the JobRunr website for marketing purposes.
         */
        @ConfigItem(defaultValue = "true")
        public boolean allowAnonymousDataUsage;
    }
}

