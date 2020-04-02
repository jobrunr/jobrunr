package org.jobrunr.jobs.filters;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.JobState;

public interface ElectStateFilter extends JobFilter {

    void onStateElection(Job job, JobState newState);

}
