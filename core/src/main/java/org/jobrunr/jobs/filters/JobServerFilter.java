package org.jobrunr.jobs.filters;

import org.jobrunr.jobs.Job;

/**
 * A filter that is triggered each time that:
 * <ul>
 *     <li>a Job starts processing</li>
 *     <li>a Job has been processed</li>
 *     <li>a Job succeeds</li>
 *     <li>a Job fails</li>
 *     <li>a Job fails after all retries are exhausted</li>
 * </ul>
 * <p>
 * Can be useful for adding extra logging, ... .
 */
public interface JobServerFilter extends JobFilter {

    /**
     * This hook is called when the Job starts processing
     *
     * @param job the job that will be processed
     */
    default void onProcessing(Job job) {
    }

    /**
     * This hook is called when the Job has been processed successfully (note that the job still has the <code>PROCESSING</code> state).
     * This method will not be called when the job failed to process.
     *
     * @param job the job that has been processed successfully.
     */
    default void onProcessed(Job job) {
    }

    /**
     * This hook is called when a Job succeeded
     *
     * @param job the job that succeeded.
     */
    default void onSucceeded(Job job) {
    }

    /**
     * This hook is called when a Job failed (but it will be retried thanks to the <code>RetryFilter</code> if the retries are not exhausted).
     *
     * @param job the job that failed.
     */
    default void onFailed(Job job) {
    }

    /**
     * This hook is called when a Job failed and will not be retried anymore (due to the fact that the retries are exhausted).
     *
     * @param job the job that failed.
     */
    default void onFailedAfterRetries(Job job) {
    }

}
