package org.jobrunr.jobs.filters;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.JobState;

/**
 * A filter that is triggered each time that the state of a Job has changed (except when the Job is deleted via the Dashboard).
 * Can be useful for adding extra logging, ... .
 * This filter will be called after that the job has been saved to a {@link org.jobrunr.storage.StorageProvider}.
 * Altering the job will not have any influence as it is not saved.
 * <p>
 * <b><em>Please note:</em></b> Any {@link JobFilter} should process really fast. If it is repeatedly slow, it'll negatively impacts the performance of JobRunr.
 */
public interface ApplyStateFilter extends JobFilter {

    /**
     * Will be invoked on state change of a {@link Job}.
     * <p>
     * <em>Note:</em> this filter may be called twice in a row in case that the {@link RetryFilter} intervened.
     * In such a case, the job provided will have in both cases the final state after the update by the {@link RetryFilter}.
     * The {@code oldState} and {@code newState} parameters will however be representing all the different state changes.
     *
     * @param job      the job for which to apply the filter.
     * @param oldState the previous state of the job - can be null
     * @param newState the new state of the job. In most cases, this will match the actual state of the job unless the {@link RetryFilter} intervened.
     */
    void onStateApplied(Job job, JobState oldState, JobState newState);
}
