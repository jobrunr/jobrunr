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
     * This hook is called when the Job processing succeeded (note that the job still has the <code>PROCESSING</code> state).
     *
     * @param job the job that has been processed successfully.
     * @deprecated Please use {@link JobServerFilter#onProcessingSucceeded(Job)}
     */
    @Deprecated
    default void onProcessed(Job job) {
    }

    /**
     * This hook is called when the Job processing succeeded (note that the job still has the <code>PROCESSING</code> state).
     *
     * @param job the job that has been processed successfully.
     */
    default void onProcessingSucceeded(Job job) {
    }

    /**
     * This hook is called when the Job processing failed (note that the job still has the <code>PROCESSING</code> state).
     *
     * @param job the job that has been processed successfully.
     * @param e   the exception that occurred.
     */
    default void onProcessingFailed(Job job, Exception e) {
    }

    /**
     * This hook is called when a Job failed and will not be retried anymore (due to the fact that the retries are exhausted).
     *
     * @param job the job that failed.
     */
    default void onFailedAfterRetries(Job job) {
    }

}
