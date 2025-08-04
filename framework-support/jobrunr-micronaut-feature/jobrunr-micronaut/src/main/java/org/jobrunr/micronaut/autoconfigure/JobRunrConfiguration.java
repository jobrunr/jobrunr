package org.jobrunr.micronaut.autoconfigure;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Context;
import io.micronaut.core.bind.annotation.Bindable;
import jakarta.validation.constraints.NotNull;
import org.jobrunr.server.configuration.BackgroundJobServerThreadType;

import java.time.Duration;
import java.util.Optional;

@Context
@ConfigurationProperties("jobrunr")
public interface JobRunrConfiguration {

    @NotNull
    DatabaseConfiguration getDatabase();

    @NotNull
    JobsConfiguration getJobs();

    @NotNull
    JobSchedulerConfiguration getJobScheduler();

    @NotNull
    BackgroundJobServerConfiguration getBackgroundJobServer();

    @NotNull
    DashboardConfiguration getDashboard();

    @NotNull
    MiscellaneousConfiguration getMiscellaneous();


    @ConfigurationProperties("jobs")
    interface JobsConfiguration {

        /**
         * Configures the default amount of retries.
         */
        Optional<Integer> getDefaultNumberOfRetries();

        /**
         * Configures the seed for the exponential back-off when jobs are retried in case of an Exception.
         */
        Optional<Integer> getRetryBackOffTimeSeed();

        /**
         * Allows to configure the MicroMeter Metrics integration for jobs.
         */
        @NotNull
        MetricsConfiguration getMetrics();
    }

    @ConfigurationProperties("database")
    interface DatabaseConfiguration {

        /**
         * Allows to skip the creation of the tables - this means you should add them manually or by database migration tools like FlywayDB or Liquibase.
         */
        @Bindable(defaultValue = "false")
        boolean isSkipCreate();

        /**
         * The name of the database to use (only used by MongoDBStorageProvider). By default, it is 'jobrunr'.
         */
        Optional<String> getDatabaseName();

        /**
         * Allows to set the table prefix used by JobRunr
         */
        Optional<String> getTablePrefix();

        /**
         * An optional named {@link javax.sql.DataSource} to use. Defaults to the 'default' datasource.
         */
        Optional<String> getDatasource();

        /**
         * If multiple types of databases are available in the Spring Context (e.g. a DataSource and a MongoDB Client), this setting allows to specify the type of database for JobRunr to use.
         * Valid values are 'sql' and 'mongodb' and 'mem'.
         */
        Optional<String> getType();
    }

    @ConfigurationProperties("jobScheduler")
    interface JobSchedulerConfiguration {

        /**
         * Enables the scheduling of jobs.
         */
        @Bindable(defaultValue = "true")
        boolean isEnabled();

        /**
         * Defines the JobDetailsGenerator to use. This should be the fully qualified classname of the
         * JobDetailsGenerator, and it should have a default no-argument constructor.
         */
        Optional<String> getJobDetailsGenerator();

    }

    @ConfigurationProperties("backgroundJobServer")
    interface BackgroundJobServerConfiguration {

        /**
         * Allows to set the name of the {@link org.jobrunr.server.BackgroundJobServer} (used in the dashboard).
         */
        Optional<String> getName();

        /**
         * Enables the background processing of jobs.
         */
        @Bindable(defaultValue = "false")
        boolean isEnabled();

        /**
         * Sets the workerCount for the BackgroundJobServer which defines the maximum number of jobs that will be run in parallel.
         * By default, this will be determined by the amount of available processor.
         */
        Optional<Integer> getWorkerCount();

        /**
         * Sets the Thread Type for the BackgroundJobServer.
         * By default, this will be determined by the Java version (VirtualThreads as of Java 21).
         */
        Optional<BackgroundJobServerThreadType> getThreadType();

        /**
         * Set the pollIntervalInSeconds for the BackgroundJobServer to see whether new jobs need to be processed
         */
        Optional<Integer> getPollIntervalInSeconds();

        /**
         * Set the pollInterval multiplicand used to determine when a BackgroundJobServer has timed out and processing jobs are orphaned.
         */
        Optional<Integer> getServerTimeoutPollIntervalMultiplicand();

        /**
         * Sets the maximum number of carbon aware jobs to update from awaiting to scheduled state per database round-trip.
         */
        Optional<Integer> getCarbonAwaitingJobsRequestSize();

        /**
         * Sets the maximum number of jobs to update from scheduled to enqueued state per database round-trip.
         */
        Optional<Integer> getScheduledJobsRequestSize();

        /**
         * Sets the query size for misfired jobs per database round-trip (to retry them).
         */
        Optional<Integer> getOrphanedJobsRequestSize();

        /**
         * Sets the maximum number of jobs to update from succeeded to deleted state per database round-trip.
         */
        Optional<Integer> getSucceededJobsRequestSize();

        /**
         * Sets the duration to wait before changing jobs that are in the SUCCEEDED state to the DELETED state.
         */
        Optional<Duration> getDeleteSucceededJobsAfter();

        /**
         * Sets the duration to wait before permanently deleting jobs that are in the DELETED state.
         */
        Optional<Duration> getPermanentlyDeleteDeletedJobsAfter();

        /**
         * Sets the duration to wait before interrupting threads/jobs when the server is stopped.
         */
        Optional<Duration> getInterruptJobsAwaitDurationOnStop();

        /**
         * Allows to configure the MicroMeter Metrics integration for the BackgroundJobServer.
         */
        @NotNull
        MetricsConfiguration getMetrics();

        @NotNull
        CarbonAwareJobProcessingConfiguration getCarbonAwareJobProcessingConfiguration();

        @ConfigurationProperties("carbonAwareJobProcessing")
        interface CarbonAwareJobProcessingConfiguration {
            /**
             * Enables the carbon aware configuration to schedule jobs optimally.
             */
            @Bindable(defaultValue = "false")
            boolean isEnabled();

            Optional<String> getAreaCode();

            Optional<String> getDataProvider();

            Optional<String> getExternalIdentifier();

            Optional<String> getExternalCode();

            Optional<Integer> getApiClientConnectTimeoutMs();

            Optional<Integer> getApiClientReadTimeoutMs();

            /**
             * Set the poll interval in minutes for the BackgroundJobServer to process carbon aware jobs
             */
            Optional<Integer> getPollIntervalInMinutes();
        }
    }

    @ConfigurationProperties("dashboard")
    interface DashboardConfiguration {

        /**
         * Enables the JobRunr dashboard.
         */
        @Bindable(defaultValue = "false")
        boolean isEnabled();

        /**
         * The port on which the Dashboard should run
         */
        Optional<Integer> getPort();

        /**
         * The username for the basic authentication which protects the dashboard
         */
        Optional<String> getUsername();

        /**
         * The password for the basic authentication which protects the dashboard. WARNING: this is insecure as it is in clear text
         */
        Optional<String> getPassword();
    }

    @ConfigurationProperties("miscellaneous")
    interface MiscellaneousConfiguration {

        /**
         * Allows to opt-out of anonymous usage statistics. This setting is true by default and sends only the total amount of succeeded jobs processed
         * by your cluster per day to show a counter on the JobRunr website for marketing purposes.
         */
        @Bindable(defaultValue = "true")
        boolean isAllowAnonymousDataUsage();
    }

    @ConfigurationProperties("metrics")
    interface MetricsConfiguration {

        /**
         * Allows to enable the MicroMeter integration
         */
        @Bindable(defaultValue = "false")
        boolean isEnabled();
    }
}
