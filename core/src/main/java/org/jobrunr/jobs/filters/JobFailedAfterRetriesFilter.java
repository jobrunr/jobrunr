package org.jobrunr.jobs.filters;

import org.jobrunr.jobs.Job;

/**
 * A filter that is triggered each time that a job has failed (after all retries are exhausted).
 * Can be useful for adding extra logging, ... .
 * This filter will be called after that the job has been saved to a {@link org.jobrunr.storage.StorageProvider}.
 * Altering the job will not have any influence as it is not saved.
 */
public interface JobFailedAfterRetriesFilter extends JobFilter {

    /**
     * @param job the job that failed
     */
    void onJobFailed(Job job);
}
