package org.jobrunr.server;

import org.jobrunr.jobs.AbstractJob;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.filters.ApplyStateFilter;
import org.jobrunr.jobs.filters.JobClientFilter;
import org.jobrunr.jobs.filters.JobServerFilter;
import org.jobrunr.jobs.states.JobState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LogAllStateChangesFilter implements ApplyStateFilter, JobClientFilter, JobServerFilter {

    private final Map<String, Boolean> onCreatingIsCalled;
    private final Map<String, Boolean> onCreatedIsCalled;
    private final Map<UUID, Boolean> onProcessingIsCalled;
    private final Map<UUID, Boolean> onProcessingSucceededIsCalled;
    private final Map<UUID, Boolean> onProcessingFailedIsCalled;
    private final Map<UUID, Boolean> onFailedAfterRetriesIsCalled;
    private final Map<UUID, Boolean> onBatchJobSucceededIsCalled;
    private final Map<UUID, Boolean> onBatchJobFailedIsCalled;
    private final Map<UUID, List<String>> stateChanges;

    public LogAllStateChangesFilter() {
        onCreatingIsCalled = new HashMap<>();
        onCreatedIsCalled = new HashMap<>();
        onProcessingIsCalled = new HashMap<>();
        onProcessingSucceededIsCalled = new HashMap<>();
        onProcessingFailedIsCalled = new HashMap<>();
        onFailedAfterRetriesIsCalled = new HashMap<>();
        onBatchJobSucceededIsCalled = new HashMap<>();
        onBatchJobFailedIsCalled = new HashMap<>();
        this.stateChanges = new HashMap<>();
    }

    @Override
    public void onStateApplied(Job job, JobState oldState, JobState newState) {
        this.stateChanges.putIfAbsent(job.getId(), new ArrayList<>());
        List<String> jobStateChanges = this.stateChanges.get(job.getId());
        if (oldState == null) {
            jobStateChanges.add("CREATED->" + newState.getName());
        } else {
            jobStateChanges.add(oldState.getName() + "->" + newState.getName());
        }
    }

    @Override
    public void onCreating(AbstractJob job) {
        this.onCreatingIsCalled.put(job.getId().toString(), true);
    }

    @Override
    public void onCreated(AbstractJob job) {
        this.onCreatedIsCalled.put(job.getId().toString(), true);
    }

    @Override
    public void onProcessing(Job job) {
        this.onProcessingIsCalled.put(job.getId(), true);
    }

    @Override
    public void onProcessingSucceeded(Job job) {
        this.onProcessingSucceededIsCalled.put(job.getId(), true);
    }

    @Override
    public void onProcessingFailed(Job job, Exception e) {
        this.onProcessingFailedIsCalled.put(job.getId(), true);
    }

    @Override
    public void onFailedAfterRetries(Job job) {
        this.onFailedAfterRetriesIsCalled.put(job.getId(), true);
    }

    public boolean onCreatingIsCalled(JobId jobId) {
        return onCreatingIsCalled.get(jobId.toString());
    }

    public boolean onCreatedIsCalled(JobId jobId) {
        return onCreatedIsCalled.get(jobId.toString());
    }

    public boolean onProcessingIsCalled(Job job) {
        return onProcessingIsCalled(job.getId());
    }

    public boolean onProcessingIsCalled(JobId jobId) {
        return onProcessingIsCalled(jobId.asUUID());
    }

    public boolean onProcessingIsCalled(UUID jobId) {
        return this.onProcessingIsCalled.getOrDefault(jobId, false);
    }

    public boolean onProcessingSucceededIsCalled(Job job) {
        return onProcessingSucceededIsCalled(job.getId());
    }

    public boolean onProcessingSucceededIsCalled(JobId jobId) {
        return onProcessingSucceededIsCalled(jobId.asUUID());
    }

    public boolean onProcessingSucceededIsCalled(UUID jobId) {
        return this.onProcessingSucceededIsCalled.getOrDefault(jobId, false);
    }

    public boolean onProcessingFailedIsCalled(Job job) {
        return onProcessingFailedIsCalled(job.getId());
    }

    public boolean onProcessingFailedIsCalled(JobId jobId) {
        return onProcessingFailedIsCalled(jobId.asUUID());
    }

    public boolean onProcessingFailedIsCalled(UUID jobId) {
        return this.onProcessingFailedIsCalled.getOrDefault(jobId, false);
    }

    public boolean onFailedAfterRetriesIsCalled(Job job) {
        return onFailedAfterRetriesIsCalled(job.getId());
    }

    public boolean onFailedAfterRetriesIsCalled(JobId jobId) {
        return onFailedAfterRetriesIsCalled(jobId.asUUID());
    }

    public boolean onFailedAfterRetriesIsCalled(UUID jobId) {
        return this.onFailedAfterRetriesIsCalled.getOrDefault(jobId, false);
    }

    public List<String> getStateChanges() {
        if (this.stateChanges.size() != 1) throw new IllegalStateException("There are more than 1 jobs with statechanges");
        return getStateChanges(this.stateChanges.keySet().iterator().next());
    }

    public List<String> getAllStateChanges() {
        List<String> allStateChanges = new ArrayList<>();
        this.stateChanges.values().forEach(allStateChanges::addAll);
        return allStateChanges;
    }

    public List<String> getStateChanges(Job job) {
        return getStateChanges(job.getId());
    }

    public List<String> getStateChanges(JobId jobId) {
        return this.getStateChanges(jobId.asUUID());
    }

    public List<String> getStateChanges(UUID jobId) {
        return this.stateChanges.getOrDefault(jobId, new ArrayList<>());
    }

    public void clear() {
        onCreatingIsCalled.clear();
        onCreatedIsCalled.clear();
        onProcessingIsCalled.clear();
        onProcessingSucceededIsCalled.clear();
        onProcessingFailedIsCalled.clear();
        onFailedAfterRetriesIsCalled.clear();
        onBatchJobSucceededIsCalled.clear();
        onBatchJobFailedIsCalled.clear();
        stateChanges.clear();
    }
}
