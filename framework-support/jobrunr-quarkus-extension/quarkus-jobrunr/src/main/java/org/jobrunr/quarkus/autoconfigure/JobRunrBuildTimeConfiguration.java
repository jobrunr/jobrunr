package org.jobrunr.quarkus.autoconfigure;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

import java.util.Optional;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "quarkus.jobrunr")
public interface JobRunrBuildTimeConfiguration {

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
     * Whether or not an health check is published in case the smallrye-health extension is present.
     */
    @WithParentName
    @ConfigDocMapKey("health.enabled")
    @WithDefault("true")
    boolean healthEnabled();

    interface DatabaseConfiguration {
        /**
         * If multiple types of databases are available in the Spring Context (e.g. a DataSource and an Elastic RestHighLevelClient), this setting allows to specify the type of database for JobRunr to use.
         * Valid values are 'sql', 'mongodb', 'documentdb', and 'elasticsearch'.
         */
        Optional<String> type();
    }

    interface JobsConfiguration {

        /**
         * Configures MicroMeter metrics related to jobs
         */
        MetricsConfiguration metrics();
    }

    @ConfigGroup
    interface JobSchedulerConfiguration {

        /**
         * Enables the scheduling of jobs.
         */
        @WithDefault("true")
        boolean enabled();
    }

    interface BackgroundJobServerConfiguration {

        /**
         * Includes the necessary resources to start the background job processing servers.
         */
        @WithDefault("true")
        boolean included();

        /**
         * Configures MicroMeter metrics related to the background job server
         */
        MetricsConfiguration metrics();
    }

    interface DashboardConfiguration {

        /**
         * Includes the necessary resources to start the dashboard webserver.
         */
        @WithDefault("true")
        boolean included();
    }

    interface MetricsConfiguration {

        /**
         * Configures whether metrics are reported to MicroMeter.
         */
        @WithDefault("false")
        boolean enabled();
    }
}

