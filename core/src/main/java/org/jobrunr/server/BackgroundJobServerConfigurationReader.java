package org.jobrunr.server;

import org.jobrunr.server.carbonaware.CarbonAwareConfigurationReader;
import org.jobrunr.server.configuration.BackgroundJobServerWorkerPolicy;
import org.jobrunr.server.configuration.ConcurrentJobModificationPolicy;

import java.time.Duration;
import java.util.UUID;

public class BackgroundJobServerConfigurationReader {

    private final BackgroundJobServerConfiguration configuration;

    public BackgroundJobServerConfigurationReader(BackgroundJobServerConfiguration configuration) {
        this.configuration = configuration;
    }

    public UUID getId() {
        return configuration.id;
    }

    public String getName() {
        return configuration.name;
    }

    public int getCarbonAwareAwaitingJobsRequestSize() {
        return configuration.carbonAwareAwaitingJobsRequestSize;
    }

    public int getScheduledJobsRequestSize() {
        return configuration.scheduledJobsRequestSize;
    }

    public int getOrphanedJobsRequestSize() {
        return configuration.orphanedJobsRequestSize;
    }

    public int getSucceededJobsRequestSize() {
        return configuration.succeededJobsRequestSize;
    }

    public Duration getPollInterval() {
        return configuration.pollInterval;
    }

    public Integer getServerTimeoutPollIntervalMultiplicand() {
        return configuration.serverTimeoutPollIntervalMultiplicand;
    }

    public Duration getDeleteSucceededJobsAfter() {
        return configuration.deleteSucceededJobsAfter;
    }

    public Duration getPermanentlyDeleteDeletedJobsAfter() {
        return configuration.permanentlyDeleteDeletedJobsAfter;
    }

    public Duration getInterruptJobsAwaitDurationOnStopBackgroundJobServer() {
        return configuration.interruptJobsAwaitDurationOnStopBackgroundJobServer;
    }

    public BackgroundJobServerWorkerPolicy getBackgroundJobServerWorkerPolicy() {
        return configuration.backgroundJobServerWorkerPolicy;
    }

    public ConcurrentJobModificationPolicy getConcurrentJobModificationPolicy() {
        return configuration.concurrentJobModificationPolicy;
    }

    public CarbonAwareConfigurationReader getCarbonAwareJobProcessingConfiguration() {
        return new CarbonAwareConfigurationReader(configuration.carbonAwareJobProcessingConfiguration);
    }
}
