package org.jobrunr.quarkus.configuation;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class JobSchedulerConfiguration {

    /**
     * Enables the scheduling of jobs.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;
}
