package org.jobrunr.storage;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.storage.listeners.StorageProviderChangeListener;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.jobrunr.storage.PageRequest.ascOnUpdatedAt;

public class StorageProviderForTest implements StorageProvider {

    private final StorageProvider storageProvider;

    public StorageProviderForTest(StorageProvider storageProvider) {
        this.storageProvider = storageProvider;
    }

    @Override
    public String getName() {
        return storageProvider.getName();
    }

    @Override
    public void addJobStorageOnChangeListener(StorageProviderChangeListener listener) {
        storageProvider.addJobStorageOnChangeListener(listener);
    }

    @Override
    public void removeJobStorageOnChangeListener(StorageProviderChangeListener listener) {
        storageProvider.removeJobStorageOnChangeListener(listener);
    }

    @Override
    public void setJobMapper(JobMapper jobMapper) {
        storageProvider.setJobMapper(jobMapper);
    }

    @Override
    public void announceBackgroundJobServer(BackgroundJobServerStatus serverStatus) {
        storageProvider.announceBackgroundJobServer(serverStatus);
    }

    @Override
    public boolean signalBackgroundJobServerAlive(BackgroundJobServerStatus serverStatus) {
        return storageProvider.signalBackgroundJobServerAlive(serverStatus);
    }

    @Override
    public void signalBackgroundJobServerStopped(BackgroundJobServerStatus serverStatus) {
        storageProvider.signalBackgroundJobServerStopped(serverStatus);
    }

    @Override
    public List<BackgroundJobServerStatus> getBackgroundJobServers() {
        return storageProvider.getBackgroundJobServers();
    }

    @Override
    public UUID getLongestRunningBackgroundJobServerId() {
        return storageProvider.getLongestRunningBackgroundJobServerId();
    }

    @Override
    public int removeTimedOutBackgroundJobServers(Instant heartbeatOlderThan) {
        return storageProvider.removeTimedOutBackgroundJobServers(heartbeatOlderThan);
    }

    @Override
    public void saveMetadata(JobRunrMetadata metadata) {
        storageProvider.saveMetadata(metadata);
    }

    @Override
    public List<JobRunrMetadata> getMetadata(String name) {
        return storageProvider.getMetadata(name);
    }

    @Override
    public JobRunrMetadata getMetadata(String name, String owner) {
        return storageProvider.getMetadata(name, owner);
    }

    @Override
    public void deleteMetadata(String name) {
        storageProvider.deleteMetadata(name);
    }

    @Override
    public Job save(Job job) {
        return storageProvider.save(job);
    }

    @Override
    public int deletePermanently(UUID id) {
        return storageProvider.deletePermanently(id);
    }

    @Override
    public Job getJobById(UUID id) {
        return storageProvider.getJobById(id);
    }

    @Override
    public List<Job> save(List<Job> jobs) {
        return storageProvider.save(jobs);
    }

    @Override
    public List<Job> getJobs(StateName state, Instant updatedBefore, PageRequest pageRequest) {
        return storageProvider.getJobs(state, updatedBefore, pageRequest);
    }

    @Override
    public List<Job> getScheduledJobs(Instant scheduledBefore, PageRequest pageRequest) {
        return storageProvider.getScheduledJobs(scheduledBefore, pageRequest);
    }

    public Long countJobs(StateName state) {
        return storageProvider.getJobPage(state, ascOnUpdatedAt(0)).getTotal();
    }

    @Override
    public List<Job> getJobs(StateName state, PageRequest pageRequest) {
        return storageProvider.getJobs(state, pageRequest);
    }

    @Override
    public Page<Job> getJobPage(StateName state, PageRequest pageRequest) {
        return storageProvider.getJobPage(state, pageRequest);
    }

    @Override
    public int deleteJobsPermanently(StateName state, Instant updatedBefore) {
        return storageProvider.deleteJobsPermanently(state, updatedBefore);
    }

    @Override
    public Set<String> getDistinctJobSignatures(StateName... states) {
        return storageProvider.getDistinctJobSignatures(states);
    }

    @Override
    public boolean exists(JobDetails jobDetails, StateName... states) {
        return storageProvider.exists(jobDetails, states);
    }

    @Override
    public boolean recurringJobExists(String recurringJobId, StateName... states) {
        return storageProvider.recurringJobExists(recurringJobId, states);
    }

    @Override
    public RecurringJob saveRecurringJob(RecurringJob recurringJob) {
        return storageProvider.saveRecurringJob(recurringJob);
    }

    @Override
    public List<RecurringJob> getRecurringJobs() {
        return storageProvider.getRecurringJobs();
    }

    @Override
    public long countRecurringJobs() {
        return storageProvider.countRecurringJobs();
    }

    @Override
    public int deleteRecurringJob(String id) {
        return storageProvider.deleteRecurringJob(id);
    }

    @Override
    public JobStats getJobStats() {
        return storageProvider.getJobStats();
    }

    @Override
    public void publishTotalAmountOfSucceededJobs(int amount) {
        storageProvider.publishTotalAmountOfSucceededJobs(amount);
    }

    @Override
    public Job getJobById(JobId jobId) {
        return storageProvider.getJobById(jobId);
    }

    @Override
    public void close() {
        storageProvider.close();
    }

    @Override
    public void setUpStorageProvider(DatabaseOptions databaseOptions) {
        storageProvider.setUpStorageProvider(databaseOptions);
    }
}
