package org.jobrunr.server;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;


public class BackgroundJobServerConfigurationAssert extends AbstractAssert<BackgroundJobServerConfigurationAssert, BackgroundJobServerConfiguration> {

    protected BackgroundJobServerConfigurationAssert(BackgroundJobServerConfiguration backgroundJobServerConfiguration) {
        super(backgroundJobServerConfiguration, BackgroundJobServerConfigurationAssert.class);
    }

    public static BackgroundJobServerConfigurationAssert assertThat(BackgroundJobServerConfiguration backgroundJobServerConfiguration) {
        return new BackgroundJobServerConfigurationAssert(backgroundJobServerConfiguration);
    }

    public BackgroundJobServerConfigurationAssert hasScheduledJobRequestSize(int scheduledJobsRequestSize) {
        Assertions.assertThat(actual.getScheduledJobsRequestSize()).isEqualTo(scheduledJobsRequestSize);
        return this;
    }

    public BackgroundJobServerConfigurationAssert hasOrphanedJobRequestSize(int orphanedJobRequestSize) {
        Assertions.assertThat(actual.getOrphanedJobsRequestSize()).isEqualTo(orphanedJobRequestSize);
        return this;
    }

    public BackgroundJobServerConfigurationAssert hasSucceededJobRequestSize(int succeededJobRequestSize) {
        Assertions.assertThat(actual.getSucceededJobsRequestSize()).isEqualTo(succeededJobRequestSize);
        return this;
    }

}