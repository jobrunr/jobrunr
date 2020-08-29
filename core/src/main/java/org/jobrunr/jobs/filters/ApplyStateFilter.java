package org.jobrunr.jobs.filters;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.JobState;

public interface ApplyStateFilter extends JobFilter {

    /**
     * @param job      the job in which to apply the filter
     * @param oldState the previous state - can be null
     * @param newState the new state
     */
    void onStateApplied(Job job, JobState oldState, JobState newState);
}
