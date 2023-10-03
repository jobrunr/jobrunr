package org.jobrunr.quarkus.autoconfigure;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

import java.time.Duration;
import java.util.Optional;

@ConfigRoot(name = "jobrunr", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class JobRunrBuildTimeConfiguration {

    public DatabaseConfiguration database;

    public JobsConfiguration jobs;

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
         * If multiple types of databases are available in the Spring Context (e.g. a DataSource and an Elastic RestHighLevelClient), this setting allows to specify the type of database for JobRunr to use.
         * Valid values are 'sql', 'mongodb', 'documentdb', and 'elasticsearch'.
         */
        @ConfigItem
        public Optional<String> type;
    }

    @ConfigGroup
    public static class JobsConfiguration {

        /**
         * Configures MicroMeter metrics related to jobs
         */
        @ConfigItem
        public MetricsConfiguration metrics;
    }

    @ConfigGroup
    public static class JobSchedulerConfiguration {

        /**
         * Enables the scheduling of jobs.
         */
        @ConfigItem(defaultValue = "true")
        public boolean enabled;
    }

    @ConfigGroup
    public static class BackgroundJobServerConfiguration {

        /**
         * Enables the background processing of jobs.
         */
        @ConfigItem(defaultValue = "false")
        public boolean enabled;

        /**
         * Configures MicroMeter metrics related to the background job server
         */
        @ConfigItem
        public MetricsConfiguration metrics;
    }

    @ConfigGroup
    public static class DashboardConfiguration {

        /**
         * Enables the JobRunr dashboard.
         */
        @ConfigItem(defaultValue = "false")
        public boolean enabled;
    }

    @ConfigGroup
    public static class MetricsConfiguration {

        /**
         * Configures whether metrics are reported to MicroMeter.
         */
        @ConfigItem(defaultValue = "false")
        public boolean enabled;
    }
}

