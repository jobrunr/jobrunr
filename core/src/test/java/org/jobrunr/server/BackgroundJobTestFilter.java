package org.jobrunr.server;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.filters.ApplyStateFilter;
import org.jobrunr.jobs.filters.JobServerFilter;
import org.jobrunr.jobs.states.JobState;

import java.util.ArrayList;
import java.util.List;

public class BackgroundJobTestFilter implements ApplyStateFilter, JobServerFilter {

    public boolean onProcessingIsCalled;
    public boolean onProcessedIsCalled;
    public boolean onProcessingSucceededIsCalled;
    public boolean onProcessingFailedIsCalled;
    public boolean onFailedAfterRetriesIsCalled;
    public List<String> stateChanges = new ArrayList<>();

    @Override
    public void onStateApplied(Job job, JobState oldState, JobState newState) {
        stateChanges.add(oldState.getName() + "->" + newState.getName());
    }

    @Override
    public void onProcessing(Job job) {
        onProcessingIsCalled = true;
    }

    @Override
    public void onProcessed(Job job) {
        onProcessedIsCalled = true;
    }

    @Override
    public void onProcessingSucceeded(Job job) {
        onProcessingSucceededIsCalled = true;
    }

    @Override
    public void onProcessingFailed(Job job, Exception e) {
        onProcessingFailedIsCalled = true;
    }

    @Override
    public void onFailedAfterRetries(Job job) {
        onFailedAfterRetriesIsCalled = true;
    }
}
