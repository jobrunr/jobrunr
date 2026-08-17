package org.jobrunr.configuration;

/**
 * This class allows configuring global job settings.
 */
public class JobConfiguration {
    JobRetentionConfiguration jobRetentionConfiguration;

    /**
     * This returns the default configuration applied to jobs globally.
     *
     * @return the default Job configuration
     */
    public static JobConfiguration usingStandardJobConfiguration() {
        return new JobConfiguration();
    }

    /**
     * Configures how long jobs are kept in storage after they reach a terminal state.
     *
     * @param jobRetentionConfiguration the job retention configuration to use
     * @return the same configuration instance which provides a fluent api
     * @see JobRetentionConfiguration
     */
    public JobConfiguration andJobRetention(JobRetentionConfiguration jobRetentionConfiguration) {
        this.jobRetentionConfiguration = jobRetentionConfiguration;
        return this;
    }
}
