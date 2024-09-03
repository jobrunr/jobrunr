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
}
