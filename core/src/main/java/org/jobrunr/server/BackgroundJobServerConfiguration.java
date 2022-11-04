package org.jobrunr.server;

import org.jobrunr.server.configuration.*;
import org.jobrunr.utils.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;

/**
 * This class allows to configure the BackgroundJobServer
 */
public class BackgroundJobServerConfiguration {

    public static final int DEFAULT_POLL_INTERVAL_IN_SECONDS = 15;
    public static final int DEFAULT_PAGE_REQUEST_SIZE = 1000;
    public static final Duration DEFAULT_DELETE_SUCCEEDED_JOBS_DURATION = Duration.ofHours(36);
    public static final Duration DEFAULT_PERMANENTLY_DELETE_JOBS_DURATION = Duration.ofHours(72);

    int scheduledJobsRequestSize = DEFAULT_PAGE_REQUEST_SIZE;
    int orphanedJobsRequestSize = DEFAULT_PAGE_REQUEST_SIZE;
    int succeededJobsRequestSize = DEFAULT_PAGE_REQUEST_SIZE;
    int pollIntervalInSeconds = DEFAULT_POLL_INTERVAL_IN_SECONDS;
    String name = getHostName();
    Duration deleteSucceededJobsAfter = DEFAULT_DELETE_SUCCEEDED_JOBS_DURATION;
    Duration permanentlyDeleteDeletedJobsAfter = DEFAULT_PERMANENTLY_DELETE_JOBS_DURATION;
    BackgroundJobServerWorkerPolicy backgroundJobServerWorkerPolicy = new DefaultBackgroundJobServerWorkerPolicy();
    ConcurrentJobModificationPolicy concurrentJobModificationPolicy = new DefaultConcurrentJobModificationPolicy();

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
     * Allows to set the name for the BackgroundJobServer
     *
     * @param name the name of this BackgroundJobServer (used in the dashboard)
     * @return the same configuration instance which provides a fluent api
     */
    public BackgroundJobServerConfiguration andName(String name) {
        if (StringUtils.isNullOrEmpty(name))
            throw new IllegalArgumentException("The name can not be blank or null");
        this.name = name;
        return this;
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
     * Allows to set the maximum number of jobs to update from scheduled to enqueued state per polling interval.
     *
     * @param scheduledJobsRequestSize maximum number of jobs to update per polling interval
     * @return the same configuration instance which provides a fluent api
     */
    public BackgroundJobServerConfiguration andScheduledJobsRequestSize(int scheduledJobsRequestSize) {
        this.scheduledJobsRequestSize = scheduledJobsRequestSize;
        return this;
    }

    /**
     * Allows to set the query size for misfired jobs per polling interval (to retry them).
     *
     * @param orphanedJobsRequestSize maximum number of misfired jobs to check per polling interval
     * @return the same configuration instance which provides a fluent api
     */
    public BackgroundJobServerConfiguration andOrphanedJobsRequestSize(int orphanedJobsRequestSize) {
        this.orphanedJobsRequestSize = orphanedJobsRequestSize;
        return this;
    }

    /**
     * Allows to set the maximum number of jobs to update from succeeded to deleted state per polling interval.
     *
     * @param succeededJobsRequestSize maximum number of jobs to update per polling interval
     * @return the same configuration instance which provides a fluent api
     */
    public BackgroundJobServerConfiguration andSucceededJobsRequestSize(int succeededJobsRequestSize) {
        this.succeededJobsRequestSize = succeededJobsRequestSize;
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

    private static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "Unable to determine hostname";
        }
    }
}
