package org.jobrunr.jobs.filters;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.JobState;

/**
 * A filter that is triggered each time that the state of a Job is changed (except when the Job is deleted via the Dashboard).
 * This filter will be called before that the job has been saved to a {@link org.jobrunr.storage.StorageProvider}.
 * Altering the job will change the lifecycle of the job - an example of this is the {@link RetryFilter} which updates jobs that are failed to scheduled again.
 */
public interface ElectStateFilter extends JobFilter {

    void onStateElection(Job job, JobState newState);

}
