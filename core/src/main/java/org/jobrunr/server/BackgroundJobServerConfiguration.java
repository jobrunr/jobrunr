package org.jobrunr.server;

import org.jobrunr.server.configuration.*;

import java.time.Duration;

/**
 * This class allows to configure the BackgroundJobServer
 */
public class BackgroundJobServerConfiguration {

    public static final int DEFAULT_POLL_INTERVAL_IN_SECONDS = 15;
    public static final Duration DEFAULT_DELETE_SUCCEEDED_JOBS_DURATION = Duration.ofHours(36);
    public static final Duration DEFAULT_PERMANENTLY_DELETE_JOBS_DURATION = Duration.ofHours(72);

    int pollIntervalInSeconds = DEFAULT_POLL_INTERVAL_IN_SECONDS;
    Duration deleteSucceededJobsAfter = DEFAULT_DELETE_SUCCEEDED_JOBS_DURATION;
    Duration permanentlyDeleteDeletedJobsAfter = DEFAULT_PERMANENTLY_DELETE_JOBS_DURATION;
    BackgroundJobServerWorkerPolicy backgroundJobServerWorkerPolicy = new DefaultBackgroundJobServerWorkerPolicy();
    ConcurrentJobModificationPolicy concurrentJobModificationPolicy = new DefaultConcurrentJobModificationPolicy();
    boolean allowAnonymousDataUsage = true;

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
        if (pollIntervalInSeconds < 5)
            throw new IllegalArgumentException("The pollIntervalInSeconds can not be smaller than 5 - otherwise it will cause to much load on your SQL/noSQL datastore.");
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

    /**
     * Allows to set the backgroundJobServerWorkerPolicy for the BackgroundJobServer. The backgroundJobServerWorkerPolicy will determine
     * the final WorkDistributionStrategy used by the BackgroundJobServer.
     *
     * @param backgroundJobServerWorkerPolicy the backgroundJobServerWorkerPolicy
     * @return the same configuration instance which provides a fluent api
     */
    public BackgroundJobServerConfiguration andBackgroundJobServerWorkerPolicy(BackgroundJobServerWorkerPolicy backgroundJobServerWorkerPolicy) {
        this.backgroundJobServerWorkerPolicy = backgroundJobServerWorkerPolicy;
        return this;
    }

    /**
     * Allows to set the duration to wait before deleting succeeded jobs
     *
     * @param duration the duration to wait before deleting successful jobs
     * @return the same configuration instance which provides a fluent api
     */
    public BackgroundJobServerConfiguration andDeleteSucceededJobsAfter(Duration duration) {
        this.deleteSucceededJobsAfter = duration;
        return this;
    }

    /**
     * Allows to set the duration to wait before permanently deleting succeeded jobs
     *
     * @param duration the duration to wait before permanently deleting successful jobs
     * @return the same configuration instance which provides a fluent api
     */
    public BackgroundJobServerConfiguration andPermanentlyDeleteDeletedJobsAfter(Duration duration) {
        this.permanentlyDeleteDeletedJobsAfter = duration;
        return this;
    }

    /**
     * Allows to set the ConcurrentJobModificationPolicy for the BackgroundJobServer. The ConcurrentJobModificationPolicy will determine
     * how the BackgroundJobServer will react to concurrent modifications the jobs.
     * <p>
     * Use with care.
     *
     * @param concurrentJobModificationPolicy the concurrentJobModificationPolicy
     * @return the same configuration instance which provides a fluent api
     */
    public BackgroundJobServerConfiguration andConcurrentJobModificationPolicy(ConcurrentJobModificationPolicy concurrentJobModificationPolicy) {
        this.concurrentJobModificationPolicy = concurrentJobModificationPolicy;
        return this;
    }

    /**
     * Allows to opt-out of anonymous usage statistics. This setting is true by default and sends only the total amount of succeeded jobs processed
     * by your cluster per day to show a counter on the JobRunr website for marketing purposes.
     *
     * @return the same configuration instance which provides a fluent api
     */
    public BackgroundJobServerConfiguration andAllowAnonymousDataUsage(boolean allowAnonymousDataUsage) {
        this.allowAnonymousDataUsage = allowAnonymousDataUsage;
        return this;
    }
}
