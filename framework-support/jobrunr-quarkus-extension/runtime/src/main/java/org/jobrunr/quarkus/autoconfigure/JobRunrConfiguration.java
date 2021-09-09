package org.jobrunr.quarkus.autoconfigure;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

import java.time.Duration;
import java.util.Optional;

@ConfigRoot(name = "jobrunr", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class JobRunrConfiguration {

    public DatabaseConfiguration database;

    public JobSchedulerConfiguration jobScheduler;

    public BackgroundJobServerConfiguration backgroundJobServer;

    public DashboardConfiguration dashboard;

    /**
     * Whether or not an health check is published in case the smallrye-health extension is present.
     */
    @ConfigItem(name = "health.enabled", defaultValue = "true")
    public boolean healthEnabled;

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
    }

    @ConfigGroup
    public static class JobSchedulerConfiguration {

        /**
         * Enables the scheduling of jobs.
         */
        @ConfigItem(defaultValue = "true")
        public boolean enabled;


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
         * Enables the background processing of jobs.
         */
        @ConfigItem(defaultValue = "false")
        public boolean enabled;

        /**
         * Sets the workerCount for the BackgroundJobServer which defines the maximum number of jobs that will be run in parallel.
         * By default, this will be determined by the amount of available processor.
         */
        @ConfigItem
        public Optional<Integer> workerCount;

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
         * Enables the JobRunr dashboard.
         */
        @ConfigItem(defaultValue = "false")
        public boolean enabled;

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
}

