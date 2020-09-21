package org.jobrunr.server;

import org.jobrunr.server.configuration.BackgroundJobServerWorkerPolicy;
import org.jobrunr.server.configuration.DefaultBackgroundJobServerWorkerPolicy;
import org.jobrunr.server.configuration.FixedSizeBackgroundJobServerWorkerPolicy;

import java.time.Duration;

/**
 * This class allows to configure the BackgroundJobServer
 */
public class BackgroundJobServerConfiguration {

    int pollIntervalInSeconds = 15;
    Duration deleteSucceededJobsAfter = Duration.ofHours(36);
    Duration permanentlyDeleteDeletedJobsAfter = Duration.ofHours(72);
    BackgroundJobServerWorkerPolicy backgroundJobServerWorkerPolicy = new DefaultBackgroundJobServerWorkerPolicy();

    private BackgroundJobServerConfiguration() {

    }

    /**
     * This returns the default configuration with the BackgroundJobServer with a poll interval of 15 seconds and a worker count based on the CPU
     *
     * @return the default JobRunrDashboard configuration
     */
    public static BackgroundJobServerConfiguration usingStandardBackgroundJobServerConfiguration() {
        return new BackgroundJobServerConfiguration();
    }

    /**
     * Allows to set the pollIntervalInSeconds for the BackgroundJobServer
     *
     * @param pollIntervalInSeconds the pollIntervalInSeconds
     * @return the same configuration instance which provides a fluent api
     */
    public BackgroundJobServerConfiguration andPollIntervalInSeconds(int pollIntervalInSeconds) {
        this.pollIntervalInSeconds = pollIntervalInSeconds;
        return this;
    }

    /**
     * Allows to set the workerCount for the BackgroundJobServer which defines the maximum number of jobs that will be run in parallel
     *
     * @param workerCount the workerCount for the BackgroundJobServer
     * @return the same configuration instance which provides a fluent api
     */
    public BackgroundJobServerConfiguration andWorkerCount(int workerCount) {
        this.backgroundJobServerWorkerPolicy = new FixedSizeBackgroundJobServerWorkerPolicy(workerCount);
        return this;
    }

    public BackgroundJobServerConfiguration andWorkerCountPolicy(BackgroundJobServerWorkerPolicy backgroundJobServerWorkerPolicy) {
        this.backgroundJobServerWorkerPolicy = backgroundJobServerWorkerPolicy;
        return this;
    }

    /**
     * Allows to set the workerCount for the BackgroundJobServer which defines the duration to wait before deleting succeeded jobs
     *
     * @param duration the duration to wait before deleting successful jobs
     * @return the same configuration instance which provides a fluent api
     */
    public BackgroundJobServerConfiguration andDeleteSucceededJobsAfter(Duration duration) {
        this.deleteSucceededJobsAfter = duration;
        return this;
    }

    /**
     * Allows to set the workerCount for the BackgroundJobServer which defines the duration to wait before permanently deleting succeeded jobs
     *
     * @param duration the duration to wait before permanently deleting successful jobs
     * @return the same configuration instance which provides a fluent api
     */
    public BackgroundJobServerConfiguration andPermanentlyDeleteDeletedJobsAfter(Duration duration) {
        this.permanentlyDeleteDeletedJobsAfter = duration;
        return this;
    }
}
