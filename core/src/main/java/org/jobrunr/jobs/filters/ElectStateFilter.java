package org.jobrunr.jobs.filters;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.AllowedJobStateStateChanges;
import org.jobrunr.jobs.states.JobState;

/**
 * A filter that is triggered each time that the state of a Job is changed (except when the Job is deleted via the Dashboard).
 * This filter will be called before that the job has been saved to a {@link org.jobrunr.storage.StorageProvider}.
 * Altering the job may change the lifecycle of the job - an example of this is the {@link RetryFilter} which updates jobs that are failed to scheduled again.
 * Every {@code ElectStateFilter} must also respect the allowed state changes. See {@link AllowedJobStateStateChanges} for more info.
 *
 * <b><em>Please note:</em></b> Any {@link JobFilter} should process really fast. If it is repeatedly slow, then it will be removed as it negatively impacts the performance of JobRunr.
 */
public interface ElectStateFilter extends JobFilter {

    void onStateElection(Job job, JobState newState);

}
