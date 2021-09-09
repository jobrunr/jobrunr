package org.jobrunr.micronaut.autoconfigure;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Context;
import io.micronaut.core.bind.annotation.Bindable;

import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Optional;

@Context
@ConfigurationProperties("jobrunr")
public interface JobRunrConfiguration {

    @NotNull
    DatabaseConfiguration getDatabase();

    @NotNull
    JobSchedulerConfiguration getJobScheduler();

    @NotNull
    BackgroundJobServerConfiguration getBackgroundJobServer();

    @NotNull
    DashboardConfiguration getDashboard();


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
         * Set the pollIntervalInSeconds for the BackgroundJobServer to see whether new jobs need to be processed
         */
        Optional<Integer> getPollIntervalInSeconds();

        /**
         * Sets the duration to wait before changing jobs that are in the SUCCEEDED state to the DELETED state.
         */
        Optional<Duration> getDeleteSucceededJobsAfter();

        /**
         * Sets the duration to wait before permanently deleting jobs that are in the DELETED state.
         */
        Optional<Duration> getPermanentlyDeleteDeletedJobsAfter();
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

    @ConfigurationProperties("health")
    interface Health {

        /**
         * Allows to disable the health check for JobRunr.
         */
        @Bindable(defaultValue = "false")
        boolean isEnabled();
    }
}