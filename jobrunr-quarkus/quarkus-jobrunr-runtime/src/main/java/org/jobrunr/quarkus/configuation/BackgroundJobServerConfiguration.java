package org.jobrunr.quarkus.configuation;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

import java.time.Duration;

@ConfigGroup
public class BackgroundJobServerConfiguration {

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
    public Integer workerCount;

    /**
     * Set the pollIntervalInSeconds for the BackgroundJobServer to see whether new jobs need to be processed
     */
    @ConfigItem(defaultValue = "15")
    public Integer pollIntervalInSeconds;

    /**
     * Sets the duration to wait before changing jobs that are in the SUCCEEDED state to the DELETED state.
     */
    @ConfigItem(defaultValue = "36")
    public Duration deleteSucceededJobsAfter;

    /**
     * Sets the duration to wait before permanently deleting jobs that are in the DELETED state.
     */
    @ConfigItem(defaultValue = "36")
    public Duration permanentlyDeleteDeletedJobsAfter;
}
