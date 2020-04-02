package org.jobrunr.storage;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.utils.resilience.RateLimiter;
import org.mockito.internal.util.reflection.Whitebox;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.jobrunr.utils.JobUtils.getJobSignature;
import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;
import static org.jobrunr.utils.resilience.RateLimiter.SECOND;

public class SimpleStorageProvider implements StorageProvider {

    private final Set<JobStorageChangeListener> onChangeListeners = new HashSet<>();

    private final RateLimiter changeListenerNotificationRateLimit;
    private volatile Map<String, RecurringJob> recurringJobs = new ConcurrentHashMap<>();
    private volatile Map<UUID, Job> jobQueue = new ConcurrentHashMap<>();
    private volatile Map<UUID, BackgroundJobServerStatus> backgroundJobServers = new ConcurrentHashMap<>();

    public SimpleStorageProvider() {
        changeListenerNotificationRateLimit = rateLimit()
                .at2Requests()
                .per(SECOND);
    }

    @Override
    public void addJobStorageOnChangeListener(JobStorageChangeListener listener) {
        onChangeListeners.add(listener);
    }

    @Override
    public void setJobMapper(JobMapper jobMapper) {

    }

    @Override
    public void announceBackgroundJobServer(BackgroundJobServerStatus serverStatus) {
        Whitebox.setInternalState(serverStatus, "lastHeartbeat", Instant.now());
        backgroundJobServers.put(serverStatus.getId(), serverStatus);
    }

    @Override
    public boolean signalBackgroundJobServerAlive(BackgroundJobServerStatus serverStatus) {
        if (!backgroundJobServers.containsKey(serverStatus.getId())) throw new ServerTimedOutException(serverStatus, new StorageException("Tha server is not there"));

        final BackgroundJobServerStatus backgroundJobServerStatus = backgroundJobServers.get(serverStatus.getId());
        Whitebox.setInternalState(backgroundJobServerStatus, "lastHeartbeat", Instant.now());
        return backgroundJobServerStatus.isRunning();
    }

    @Override
    public List<BackgroundJobServerStatus> getBackgroundJobServers() {
        return new ArrayList<>(backgroundJobServers.values());
    }

    @Override
    public int removeTimedOutBackgroundJobServers(Instant heartbeatOlderThan) {
//        final String servers = backgroundJobServers.values().stream().map(serverStatus -> serverStatus.getId().toString() + ": " + serverStatus.getLastHeartbeat()).collect(Collectors.joining("\n\t"));
//        System.out.println("Evaluating server:\n\t" + servers);
        final List<UUID> serversToRemove = backgroundJobServers.entrySet().stream()
                .filter(entry -> entry.getValue().getLastHeartbeat().isBefore(heartbeatOlderThan))
                .map(entry -> entry.getKey())
                .collect(toList());
        backgroundJobServers.keySet().removeAll(serversToRemove);
        return serversToRemove.size();
    }

    @Override
    public Job getJobById(UUID id) {
        if (!jobQueue.containsKey(id)) throw new JobNotFoundException(id);

        return jobQueue.get(id);
    }

    @Override
    public Job save(Job job) {
        if (job.getId() == null) {
            job.setId(UUID.randomUUID());
        }
        jobQueue.put(job.getId(), job);
        notifyOnChangeListeners();
        return job;
    }

    @Override
    public int delete(UUID id) {
        int amountDeleted = jobQueue.remove(id) != null ? 1 : 0;
        if (amountDeleted > 0) notifyOnChangeListeners();
        return amountDeleted;
    }

    @Override
    public List<Job> save(List<Job> jobs) {
        jobs.stream()
                .forEach(job -> save(job));
        notifyOnChangeListeners();
        return jobs;
    }

    @Override
    public List<Job> getJobs(StateName state, Instant updatedBefore, PageRequest pageRequest) {
        return jobQueue.values().stream()
                .filter(job -> job.hasState(state) && job.getUpdatedAt().isBefore(updatedBefore))
                .skip(pageRequest.getOffset())
                .limit(pageRequest.getLimit())
                .collect(toList());
    }

    @Override
    public List<Job> getScheduledJobs(Instant scheduledBefore, PageRequest pageRequest) {
        return getJobs(StateName.SCHEDULED)
                .filter(job -> ((ScheduledState) job.getJobState()).getScheduledAt().isBefore(scheduledBefore))
                .skip(pageRequest.getOffset())
                .limit(pageRequest.getLimit())
                .collect(toList());
    }

    @Override
    public Long countJobs(StateName state) {
        return getJobs(state).count();
    }

    @Override
    public List<Job> getJobs(StateName state, PageRequest pageRequest) {
        return getJobs(state)
                .skip(pageRequest.getOffset())
                .limit(pageRequest.getLimit())
                .collect(toList());
    }

    @Override
    public Page<Job> getJobPage(StateName state, PageRequest pageRequest) {
        return new Page<>(countJobs(state), getJobs(state, pageRequest),
                pageRequest
        );
    }

    @Override
    public int deleteJobs(StateName state, Instant updatedBefore) {
        List<UUID> jobsToRemove = jobQueue.values().stream()
                .filter(job -> job.hasState(state))
                .filter(job -> job.getUpdatedAt().isBefore(updatedBefore))
                .map(Job::getId)
                .collect(toList());
        jobQueue.keySet().removeAll(jobsToRemove);
        notifyOnChangeListeners();
        return jobsToRemove.size();
    }

    @Override
    public boolean exists(JobDetails jobDetails, StateName state) {
        String actualJobSignature = getJobSignature(jobDetails);
        return jobQueue.values().stream()
                .filter(job -> actualJobSignature.equals(getJobSignature(job.getJobDetails())))
                .anyMatch(job -> job.hasState(state));
    }

    @Override
    public RecurringJob saveRecurringJob(RecurringJob recurringJob) {
        recurringJobs.put(recurringJob.getId(), recurringJob);
        return recurringJob;
    }

    @Override
    public List<RecurringJob> getRecurringJobs() {
        return new ArrayList<>(recurringJobs.values());
    }

    @Override
    public int deleteRecurringJob(String id) {
        recurringJobs.remove(id);
        return 0;
    }

    @Override
    public JobStats getJobStats() {
        return new JobStats(
                (long) jobQueue.size(),
                getJobs(StateName.AWAITING).count(),
                getJobs(StateName.SCHEDULED).count(),
                getJobs(StateName.ENQUEUED).count(),
                getJobs(StateName.PROCESSING).count(),
                getJobs(StateName.FAILED).count(),
                getJobs(StateName.SUCCEEDED).count(),
                backgroundJobServers.size()
        );
    }

    @Override
    public void publishJobStatCounter(StateName state, int amount) {

    }

    private Stream<Job> getJobs(StateName state) {
        return jobQueue.values().stream()
                .filter(job -> job.hasState(state))
                .sorted(Comparator.comparing(Job::getCreatedAt));
    }

    private void notifyOnChangeListeners() {
        if (changeListenerNotificationRateLimit.isRateLimited()) return;

        JobStats jobStats = getJobStats();
        onChangeListeners.forEach(listener -> listener.onChange(jobStats));
    }
}
