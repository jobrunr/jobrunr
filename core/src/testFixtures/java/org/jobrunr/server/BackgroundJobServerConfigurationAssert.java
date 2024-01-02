package org.jobrunr.server;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.jobrunr.server.configuration.BackgroundJobServerWorkerPolicy;

import java.time.Duration;


public class BackgroundJobServerConfigurationAssert extends AbstractAssert<BackgroundJobServerConfigurationAssert, BackgroundJobServerConfiguration> {

    protected BackgroundJobServerConfigurationAssert(BackgroundJobServerConfiguration backgroundJobServerConfiguration) {
        super(backgroundJobServerConfiguration, BackgroundJobServerConfigurationAssert.class);
    }

    public static BackgroundJobServerConfigurationAssert assertThat(BackgroundJobServerConfiguration backgroundJobServerConfiguration) {
        return new BackgroundJobServerConfigurationAssert(backgroundJobServerConfiguration);
    }

    public BackgroundJobServerConfigurationAssert hasName(String name) {
        Assertions.assertThat(actual.getName()).isEqualTo(name);
        return this;
    }

    public BackgroundJobServerConfigurationAssert hasPollIntervalInSeconds(int pollIntervalInSeconds) {
        Assertions.assertThat(actual.getPollInterval()).isEqualTo(Duration.ofSeconds(pollIntervalInSeconds));
        return this;
    }

    public BackgroundJobServerConfigurationAssert hasPollInterval(Duration pollInterval) {
        Assertions.assertThat(actual.getPollInterval()).isEqualTo(pollInterval);
        return this;
    }

    public BackgroundJobServerConfigurationAssert hasWorkerCount(Integer workerCount) {
        BackgroundJobServerWorkerPolicy backgroundJobServerWorkerPolicy = actual.getBackgroundJobServerWorkerPolicy();
        Assertions.assertThat(backgroundJobServerWorkerPolicy.toWorkDistributionStrategy(null).getWorkerCount()).isEqualTo(workerCount);
        return this;
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