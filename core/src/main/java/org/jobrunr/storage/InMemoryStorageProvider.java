package org.jobrunr.storage;

import org.jobrunr.jobs.*;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.utils.resilience.RateLimiter;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static java.lang.Long.parseLong;
import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.jobrunr.jobs.states.StateName.*;
import static org.jobrunr.storage.StorageProviderUtils.Metadata.*;
import static org.jobrunr.storage.StorageProviderUtils.returnConcurrentModifiedJobs;
import static org.jobrunr.utils.JobUtils.getJobSignature;
import static org.jobrunr.utils.reflection.ReflectionUtils.getValueFromFieldOrProperty;
import static org.jobrunr.utils.reflection.ReflectionUtils.setFieldUsingAutoboxing;
import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;
import static org.jobrunr.utils.resilience.RateLimiter.SECOND;

public class InMemoryStorageProvider extends AbstractStorageProvider {

    private final Map<UUID, Job> jobQueue = new ConcurrentHashMap<>();
    private final Map<UUID, BackgroundJobServerStatus> backgroundJobServers = new ConcurrentHashMap<>();
    private final List<RecurringJob> recurringJobs = new CopyOnWriteArrayList<>();
    private final Map<String, JobRunrMetadata> metadata = new ConcurrentHashMap<>();
    private JobMapper jobMapper;

    public InMemoryStorageProvider() {
        this(rateLimit().at1Request().per(SECOND));
    }

    public InMemoryStorageProvider(RateLimiter rateLimiter) {
        super(rateLimiter);
        publishTotalAmountOfSucceededJobs(0);
    }

    @Override
    public void setJobMapper(JobMapper jobMapper) {
        this.jobMapper = jobMapper;
    }

    @Override
    public void setUpStorageProvider(DatabaseOptions databaseOptions) {
        // nothing to do for InMemoryStorageProvider
    }

    @Override
    public void announceBackgroundJobServer(BackgroundJobServerStatus serverStatus) {
        final BackgroundJobServerStatus backgroundJobServerStatus = new BackgroundJobServerStatus(
                serverStatus.getId(),
                serverStatus.getName(),
                serverStatus.getWorkerPoolSize(),
                serverStatus.getPollIntervalInSeconds(),
                serverStatus.getDeleteSucceededJobsAfter(),
                serverStatus.getPermanentlyDeleteDeletedJobsAfter(),
                serverStatus.getFirstHeartbeat(),
                serverStatus.getLastHeartbeat(),
                serverStatus.isRunning(),
                serverStatus.getSystemTotalMemory(),
                serverStatus.getSystemFreeMemory(),
                serverStatus.getSystemCpuLoad(),
                serverStatus.getProcessMaxMemory(),
                serverStatus.getProcessFreeMemory(),
                serverStatus.getProcessAllocatedMemory(),
                serverStatus.getProcessCpuLoad()
        );
        backgroundJobServers.put(serverStatus.getId(), backgroundJobServerStatus);
    }

    @Override
    public boolean signalBackgroundJobServerAlive(BackgroundJobServerStatus serverStatus) {
        if (!backgroundJobServers.containsKey(serverStatus.getId()))
            throw new ServerTimedOutException(serverStatus, new StorageException("Tha server is not there"));

        announceBackgroundJobServer(serverStatus);
        final BackgroundJobServerStatus backgroundJobServerStatus = backgroundJobServers.get(serverStatus.getId());
        return backgroundJobServerStatus.isRunning();
    }

    @Override
    public void signalBackgroundJobServerStopped(BackgroundJobServerStatus serverStatus) {
        backgroundJobServers.remove(serverStatus.getId());
    }

    @Override
    public List<BackgroundJobServerStatus> getBackgroundJobServers() {
        return backgroundJobServers.values().stream()
                .sorted(comparing(BackgroundJobServerStatus::getFirstHeartbeat))
                .collect(toList());
    }

    @Override
    public UUID getLongestRunningBackgroundJobServerId() {
        return backgroundJobServers.values().stream()
                .min(comparing(BackgroundJobServerStatus::getFirstHeartbeat)).map(BackgroundJobServerStatus::getId)
                .orElseThrow(() -> new IllegalStateException("No servers available?!"));
    }

    @Override
    public int removeTimedOutBackgroundJobServers(Instant heartbeatOlderThan) {
        final List<UUID> serversToRemove = backgroundJobServers.entrySet().stream()
                .filter(entry -> entry.getValue().getLastHeartbeat().isBefore(heartbeatOlderThan))
                .map(Map.Entry::getKey)
                .collect(toList());
        backgroundJobServers.keySet().removeAll(serversToRemove);
        return serversToRemove.size();
    }

    @Override
    public Job getJobById(UUID id) {
        if (!jobQueue.containsKey(id)) throw new JobNotFoundException(id);
        return deepClone(jobQueue.get(id));
    }

    @Override
    public void saveMetadata(JobRunrMetadata metadata) {
        this.metadata.put(metadata.getName() + "-" + metadata.getOwner(), metadata);
        notifyMetadataChangeListeners();
    }

    @Override
    public List<JobRunrMetadata> getMetadata(String key) {
        return this.metadata.values().stream().filter(m -> m.getName().equals(key)).collect(toList());
    }

    @Override
    public JobRunrMetadata getMetadata(String key, String owner) {
        return this.metadata.get(key + "-" + owner);
    }

    @Override
    public void deleteMetadata(String key) {
        List<String> metadataToRemove = this.metadata.values().stream()
                .filter(metadata -> metadata.getName().equals(key))
                .map(JobRunrMetadata::getId)
                .collect(toList());
        if (!metadataToRemove.isEmpty()) {
            this.metadata.keySet().removeAll(metadataToRemove);
            notifyMetadataChangeListeners();
        }
    }

    @Override
    public Job save(Job job) {
        saveJob(job);
        notifyJobStatsOnChangeListeners();
        return job;
    }

    @Override
    public int deletePermanently(UUID id) {
        boolean removed = jobQueue.keySet().remove(id);
        notifyJobStatsOnChangeListenersIf(removed);
        return removed ? 1 : 0;
    }

    @Override
    public List<Job> save(List<Job> jobs) {
        final List<Job> concurrentModifiedJobs = returnConcurrentModifiedJobs(jobs, this::saveJob);
        if (!concurrentModifiedJobs.isEmpty()) {
            throw new ConcurrentJobModificationException(concurrentModifiedJobs);
        }
        notifyJobStatsOnChangeListeners();
        return jobs;
    }

    @Override
    public List<Job> getJobs(StateName state, Instant updatedBefore, PageRequest pageRequest) {
        return getJobsStream(state, pageRequest)
                .filter(job -> job.getUpdatedAt().isBefore(updatedBefore))
                .skip(pageRequest.getOffset())
                .limit(pageRequest.getLimit())
                .map(this::deepClone)
                .collect(toList());
    }

    @Override
    public List<Job> getScheduledJobs(Instant scheduledBefore, PageRequest pageRequest) {
        return getJobsStream(SCHEDULED, pageRequest)
                .filter(job -> ((ScheduledState) job.getJobState()).getScheduledAt().isBefore(scheduledBefore))
                .skip(pageRequest.getOffset())
                .limit(pageRequest.getLimit())
                .map(this::deepClone)
                .collect(toList());
    }

    @Override
    public List<Job> getJobs(StateName state, PageRequest pageRequest) {
        return getJobsStream(state, pageRequest)
                .skip(pageRequest.getOffset())
                .limit(pageRequest.getLimit())
                .map(this::deepClone)
                .collect(toList());
    }

    @Override
    public Page<Job> getJobPage(StateName state, PageRequest pageRequest) {
        return new Page<>(getJobsStream(state).count(), getJobs(state, pageRequest),
                pageRequest
        );
    }

    @Override
    public int deleteJobsPermanently(StateName state, Instant updatedBefore) {
        List<UUID> jobsToRemove = jobQueue.values().stream()
                .filter(job -> job.hasState(state))
                .filter(job -> job.getUpdatedAt().isBefore(updatedBefore))
                .map(Job::getId)
                .collect(toList());
        jobQueue.keySet().removeAll(jobsToRemove);
        notifyJobStatsOnChangeListenersIf(!jobsToRemove.isEmpty());
        return jobsToRemove.size();
    }

    @Override
    public Set<String> getDistinctJobSignatures(StateName... states) {
        return jobQueue.values().stream()
                .filter(job -> asList(states).contains(job.getState()))
                .map(AbstractJob::getJobSignature)
                .collect(toSet());
    }

    @Override
    public boolean exists(JobDetails jobDetails, StateName... states) {
        String actualJobSignature = getJobSignature(jobDetails);
        return jobQueue.values().stream()
                .anyMatch(job ->
                        asList(states).contains(job.getState())
                                && actualJobSignature.equals(getJobSignature(job.getJobDetails())));
    }

    @Override
    public boolean recurringJobExists(String recurringJobId, StateName... states) {
        return jobQueue.values().stream()
                .anyMatch(job ->
                        asList(states).contains(job.getState())
                                && job.getRecurringJobId()
                                .map(actualRecurringJobId -> actualRecurringJobId.equals(recurringJobId))
                                .orElse(false));
    }

    @Override
    public RecurringJob saveRecurringJob(RecurringJob recurringJob) {
        deleteRecurringJob(recurringJob.getId());
        recurringJobs.add(recurringJob);
        return recurringJob;
    }

    @Override
    public RecurringJobsResult getRecurringJobs() {
        return new RecurringJobsResult(recurringJobs);
    }

    @Override
    @Deprecated
    public long countRecurringJobs() {
        return recurringJobs.size();
    }

    @Override
    public boolean recurringJobsUpdated(Long recurringJobsUpdatedHash) {
        Long currentResult = recurringJobs.stream().map(rj -> rj.getCreatedAt().toEpochMilli()).reduce(Long::sum).orElse(0L);
        return !currentResult.equals(recurringJobsUpdatedHash);
    }

    @Override
    public int deleteRecurringJob(String id) {
        recurringJobs.removeIf(job -> id.equals(job.getId()));
        return 0;
    }

    @Override
    public JobStats getJobStats() {
        return new JobStats(
                Instant.now(),
                (long) jobQueue.size(),
                getJobsStream(SCHEDULED).count(),
                getJobsStream(ENQUEUED).count(),
                getJobsStream(PROCESSING).count(),
                getJobsStream(FAILED).count(),
                getJobsStream(SUCCEEDED).count(),
                getMetadata(STATS_NAME, STATS_OWNER).getValueAsLong(),
                getJobsStream(DELETED).count(),
                recurringJobs.size(),
                backgroundJobServers.size()
        );
    }

    @Override
    public void publishTotalAmountOfSucceededJobs(int amount) {
        JobRunrMetadata metadata = this.metadata.computeIfAbsent(STATS_ID, input -> new JobRunrMetadata(STATS_NAME, STATS_OWNER, new AtomicLong(0).toString()));
        metadata.setValue(new AtomicLong(parseLong(metadata.getValue()) + amount).toString());
    }

    private Stream<Job> getJobsStream(StateName state, PageRequest pageRequest) {
        return getJobsStream(state)
                .sorted(getJobComparator(pageRequest));
    }

    private Stream<Job> getJobsStream(StateName state) {
        return jobQueue.values().stream()
                .filter(job -> job.hasState(state));
    }

    private Job deepClone(Job job) {
        final String serializedJobAsString = jobMapper.serializeJob(job);
        final Job result = jobMapper.deserializeJob(serializedJobAsString);
        setFieldUsingAutoboxing("locker", result, getValueFromFieldOrProperty(job, "locker"));
        return result;
    }

    private synchronized void saveJob(Job job) {
        final Job oldJob = jobQueue.get(job.getId());
        if ((oldJob != null && job.getVersion() != oldJob.getVersion()) || (oldJob == null && job.getVersion() > 0)) {
            throw new ConcurrentJobModificationException(job);
        }

        try (JobVersioner jobVersioner = new JobVersioner(job)) {
            jobQueue.put(job.getId(), deepClone(job));
            jobVersioner.commitVersion();
        }
    }

    private Comparator<Job> getJobComparator(PageRequest pageRequest) {
        List<Comparator<Job>> result = new ArrayList<>();
        final String[] sortOns = pageRequest.getOrder().split(",");
        for (String sortOn : sortOns) {
            final String[] sortAndOrder = sortOn.split(":");
            String sortField = sortAndOrder[0];
            PageRequest.Order order = PageRequest.Order.ASC;
            if (sortAndOrder.length > 1) {
                order = PageRequest.Order.valueOf(sortAndOrder[1].toUpperCase());
            }
            Comparator<Job> comparator = null;
            if (sortField.equalsIgnoreCase(FIELD_CREATED_AT)) {
                comparator = Comparator.comparing(Job::getCreatedAt);
            } else if (sortField.equalsIgnoreCase(FIELD_UPDATED_AT)) {
                comparator = Comparator.comparing(Job::getUpdatedAt);
            } else {
                throw new IllegalStateException("An unsupported sortOrder was requested: " + sortField);
            }
            if (order == PageRequest.Order.DESC) {
                comparator = comparator.reversed();
            }
            result.add(comparator);
        }
        return result.stream()
                .reduce(Comparator::thenComparing)
                .orElse((a, b) -> 0); // default order
    }

}
