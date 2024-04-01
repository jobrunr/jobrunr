package org.jobrunr.quarkus.autoconfigure;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import org.jobrunr.server.configuration.BackgroundJobServerThreadType;

import java.time.Duration;
import java.util.Optional;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.jobrunr")
public interface JobRunrRuntimeConfiguration {

    /**
     * Allows to configure JobRunr database related settings
     */
    DatabaseConfiguration database();

    /**
     * Allows to configure JobRunr job related settings
     */
    JobsConfiguration jobs();

    /**
     * Allows to configure JobRunr JobScheduler related settings
     */
    JobSchedulerConfiguration jobScheduler();

    /**
     * Allows to configure JobRunr BackgroundJobServer related settings
     */
    BackgroundJobServerConfiguration backgroundJobServer();

    /**
     * Allows to configure JobRunr Dashboard related settings
     */
    DashboardConfiguration dashboard();

    /**
     * Allows to configure miscellaneous settings related to JobRunr
     */
    MiscellaneousConfiguration miscellaneous();

    interface DatabaseConfiguration {

        /**
         * Allows to skip the creation of the tables - this means you should add them manually or by database migration tools like FlywayDB or Liquibase.
         */
        @WithDefault("false")
        boolean skipCreate();

        /**
         * Allows to set the database name to use (only used by MongoDBStorageProvider). By default, it is 'jobrunr'.
         */
        Optional<String> databaseName();

        /**
         * Allows to set the table prefix used by JobRunr
         */
        Optional<String> tablePrefix();

        /**
         * An optional named {@link javax.sql.DataSource} to use. Defaults to the 'default' datasource.
         */
        Optional<String> datasource();

        /**
         * If multiple types of databases are available in the Spring Context (e.g. a DataSource and an Elastic RestHighLevelClient), this setting allows to specify the type of database for JobRunr to use.
         * Valid values are 'sql', 'mongodb', 'documentdb', and 'elasticsearch'.
         */
        Optional<String> type();
    }

    interface JobsConfiguration {

        /**
         * Configures the default amount of retries.
         */
        Optional<Integer> defaultNumberOfRetries();

        /**
         * Configures the seed for the exponential back-off when jobs are retried in case of an Exception.
         */
        Optional<Integer> retryBackOffTimeSeed();
    }

    interface JobSchedulerConfiguration {
        /**
         * Defines the JobDetailsGenerator to use. This should be the fully qualified classname of the
         * JobDetailsGenerator, and it should have a default no-argument constructor.
         */
        Optional<String> jobDetailsGenerator();
    }

    interface BackgroundJobServerConfiguration {

        /**
         * Sets the name of the {@link org.jobrunr.server.BackgroundJobServer} (used in the dashboard).
         */
        Optional<String> name();

        /**
         * Sets the workerCount for the BackgroundJobServer which defines the maximum number of jobs that will be run in parallel.
         * By default, this will be determined by the amount of available processor.
         */
        Optional<Integer> workerCount();

        /**
         * Sets the Thread Type for the BackgroundJobServer.
         * By default, this will be determined by the Java version (VirtualThreads as of Java 21).
         */
        Optional<BackgroundJobServerThreadType> threadType();

        /**
         * Sets the maximum number of jobs to update from awaiting to scheduled state per X hours
         */
        Optional<Integer> carbonAwatingJobsRequestSize();

        /**
         * Sets the maximum number of jobs to update from scheduled to enqueued state per polling interval.
         */
        Optional<Integer> scheduledJobsRequestSize();

        /**
         * Sets the query size for misfired jobs per polling interval (to retry them).
         */
        Optional<Integer> orphanedJobsRequestSize();

        /**
         * Sets the maximum number of jobs to update from succeeded to deleted state per polling interval.
         */
        Optional<Integer> succeededJobRequestSize();

        /**
         * Set the pollIntervalInSeconds for the BackgroundJobServer to see whether new jobs need to be processed
         */
        Optional<Integer> pollIntervalInSeconds();

        /**
         * Sets the duration to wait before changing jobs that are in the SUCCEEDED state to the DELETED state.
         */
        Optional<Duration> deleteSucceededJobsAfter();

        /**
         * Sets the duration to wait before permanently deleting jobs that are in the DELETED state.
         */
        Optional<Duration> permanentlyDeleteDeletedJobsAfter();

        /**
         * Sets the duration to wait before interrupting threads/jobs when the server is stopped.
         */
        Optional<Duration> interruptJobsAwaitDurationOnStop();
    }

    interface DashboardConfiguration {

        /**
         * The port on which the Dashboard should run
         */
        Optional<Integer> port();

        /**
         * The username for the basic authentication which protects the dashboard
         */
        Optional<String> username();

        /**
         * The password for the basic authentication which protects the dashboard. WARNING: this is insecure as it is in clear text
         */
        Optional<String> password();
    }

    interface MiscellaneousConfiguration {

        /**
         * Allows to opt-out of anonymous usage statistics. This setting is true by default and sends only the total amount of succeeded jobs processed
         * by your cluster per day to show a counter on the JobRunr website for marketing purposes.
         */
        @WithDefault("true")
        boolean allowAnonymousDataUsage();
    }
}

