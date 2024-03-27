package org.jobrunr.server;

import org.jobrunr.server.configuration.BackgroundJobServerWorkerPolicy;
import org.jobrunr.server.configuration.ConcurrentJobModificationPolicy;
import org.jobrunr.server.configuration.DefaultBackgroundJobServerWorkerPolicy;
import org.jobrunr.server.configuration.DefaultConcurrentJobModificationPolicy;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.UUID;

import static java.lang.Math.min;
import static org.jobrunr.utils.StringUtils.isNullOrEmpty;

/**
 * This class allows to configure the BackgroundJobServer
 */
public class BackgroundJobServerConfiguration {

    public static final Duration DEFAULT_POLL_INTERVAL = Duration.ofSeconds(15);
    public static final int DEFAULT_SERVER_TIMEOUT_POLL_INTERVAL_MULTIPLICAND = 4;
    public static final int DEFAULT_PAGE_REQUEST_SIZE = 1000;
    public static final Duration DEFAULT_DELETE_SUCCEEDED_JOBS_DURATION = Duration.ofHours(36);
    public static final Duration DEFAULT_PERMANENTLY_DELETE_JOBS_DURATION = Duration.ofHours(72);
    public static final Duration DEFAULT_INTERRUPT_JOBS_AWAIT_DURATION_ON_STOP_BACKGROUND_JOB_SERVER = Duration.ofSeconds(10);

    int scheduledJobsRequestSize = DEFAULT_PAGE_REQUEST_SIZE;
    int orphanedJobsRequestSize = DEFAULT_PAGE_REQUEST_SIZE;
    int succeededJobsRequestSize = DEFAULT_PAGE_REQUEST_SIZE;
    Duration pollInterval = DEFAULT_POLL_INTERVAL;
    int serverTimeoutPollIntervalMultiplicand = DEFAULT_SERVER_TIMEOUT_POLL_INTERVAL_MULTIPLICAND;
    UUID id = UUID.randomUUID();
    String name = getHostName();
    Duration deleteSucceededJobsAfter = DEFAULT_DELETE_SUCCEEDED_JOBS_DURATION;
    Duration permanentlyDeleteDeletedJobsAfter = DEFAULT_PERMANENTLY_DELETE_JOBS_DURATION;
    Duration interruptJobsAwaitDurationOnStopBackgroundJobServer = DEFAULT_INTERRUPT_JOBS_AWAIT_DURATION_ON_STOP_BACKGROUND_JOB_SERVER;
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
     * Allows to set the id for the {@link BackgroundJobServer}
     *
     * @param id the id of this BackgroundJobServer (used in the dashboard and to see whether server is still up &amp; running)
     * @return the same configuration instance which provides a fluent api
     */
    public BackgroundJobServerConfiguration andId(UUID id) {
        if (id == null) throw new IllegalArgumentException("The id can not be null");
        this.id = id;
        return this;
    }

    /**
     * Allows to set the name for the {@link BackgroundJobServer}
     *
     * @param name the name of this BackgroundJobServer (used in the dashboard)
     * @return the same configuration instance which provides a fluent api
     */
    public BackgroundJobServerConfiguration andName(String name) {
        if (isNullOrEmpty(name)) throw new IllegalArgumentException("The name can not be null or empty");
        if (name.length() >= 128) throw new IllegalArgumentException("The length of the name can not exceed 128 characters");
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
        return this.andPollInterval(Duration.ofSeconds(pollIntervalInSeconds));
    }

    /**
     * Allows to set the pollInterval duration for the BackgroundJobServer
     *
     * @param pollInterval the pollInterval duration
     * @return the same configuration instance which provides a fluent api
     */
    public BackgroundJobServerConfiguration andPollInterval(Duration pollInterval) {
        this.pollInterval = pollInterval;
        return this;
    }

    /**
     * Allows to set the pollInterval multiplicand after which a BackgroundJobServer will be seen as timed out (e.g. because it crashed, was stopped, ...).
     * <p>
     * You can increase this value if you have long stop the world GC cycles or are running on shared hosting and experiencing CPU starvation.
     *
     * @param multiplicand the pollInterval multiplicand
     * @return the same configuration instance which provides a fluent api
     */
    public BackgroundJobServerConfiguration andServerTimeoutPollIntervalMultiplicand(int multiplicand) {
        if (multiplicand < 4) throw new IllegalArgumentException("The smallest possible ServerTimeoutPollIntervalMultiplicand is 4 (4 is also the default)");
        this.serverTimeoutPollIntervalMultiplicand = multiplicand;
        return this;
    }

    /**
     * Allows to set the workerCount for the BackgroundJobServer which defines the maximum number of jobs that will be run in parallel
     *
     * @param workerCount the workerCount for the BackgroundJobServer
     * @return the same configuration instance which provides a fluent api
     */
    public BackgroundJobServerConfiguration andWorkerCount(int workerCount) {
        this.backgroundJobServerWorkerPolicy = new DefaultBackgroundJobServerWorkerPolicy(workerCount);
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
     * Allows to set the duration to wait before interrupting jobs/threads when the {@link BackgroundJobServer} is stopped
     *
     * @param duration the duration to wait before interrupting jobs/threads when the {@link BackgroundJobServer} is stopped
     * @return the same configuration instance which provides a fluent api
     */
    public BackgroundJobServerConfiguration andInterruptJobsAwaitDurationOnStopBackgroundJobServer(Duration duration) {
        this.interruptJobsAwaitDurationOnStopBackgroundJobServer = duration;
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
            String hostName = InetAddress.getLocalHost().getHostName();
            return hostName.substring(0, min(hostName.length(), 127));
        } catch (UnknownHostException e) {
            return "Unable to determine hostname";
        }
    }
}
