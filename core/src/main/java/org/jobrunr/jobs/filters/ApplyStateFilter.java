package org.jobrunr.jobs.filters;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.JobState;

public interface ApplyStateFilter extends JobFilter {

    /**
     * @param job
     * @param oldState can be null
     * @param newState
     */
    void onStateApplied(Job job, JobState oldState, JobState newState);
}
