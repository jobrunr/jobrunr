package org.jobrunr.storage;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.utils.resilience.RateLimiter;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.jobrunr.jobs.states.StateName.AWAITING;
import static org.jobrunr.jobs.states.StateName.DELETED;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.FAILED;
import static org.jobrunr.jobs.states.StateName.PROCESSING;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;
import static org.jobrunr.utils.JobUtils.getJobSignature;
import static org.jobrunr.utils.reflection.ReflectionUtils.getValueFromFieldOrProperty;
import static org.jobrunr.utils.reflection.ReflectionUtils.setFieldUsingAutoboxing;
import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;
import static org.jobrunr.utils.resilience.RateLimiter.SECOND;

public class InMemoryStorageProvider extends AbstractStorageProvider {

    private final Map<UUID, Job> jobQueue = new ConcurrentHashMap<>();
    private final Map<UUID, BackgroundJobServerStatus> backgroundJobServers = new ConcurrentHashMap<>();
    private final List<RecurringJob> recurringJobs = new CopyOnWriteArrayList<>();
    private final Map<Object, AtomicLong> jobStats = new ConcurrentHashMap<>();
    private JobMapper jobMapper;

    public InMemoryStorageProvider() {
        this(rateLimit().at2Requests().per(SECOND));
    }

    public InMemoryStorageProvider(RateLimiter rateLimiter) {
        super(rateLimiter);
    }

    @Override
    public void setJobMapper(JobMapper jobMapper) {
        this.jobMapper = jobMapper;
    }

    @Override
    public void announceBackgroundJobServer(BackgroundJobServerStatus serverStatus) {
        final BackgroundJobServerStatus backgroundJobServerStatus = new BackgroundJobServerStatus(
                serverStatus.getId(),
                serverStatus.getWorkerPoolSize(),
                serverStatus.getPollIntervalInSeconds(),
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
        if (!backgroundJobServers.containsKey(serverStatus.getId())) throw new ServerTimedOutException(serverStatus, new StorageException("Tha server is not there"));

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
    public Job save(Job job) {
        saveJob(job);
        notifyJobStatsOnChangeListeners();
        return job;
    }

    @Override
    public int deletePermanently(UUID id) {
        int amountDeleted = jobQueue.remove(id) != null ? 1 : 0;
        if (amountDeleted > 0) notifyJobStatsOnChangeListeners();
        return amountDeleted;
    }

    @Override
    public List<Job> save(List<Job> jobs) {
        jobs
                .forEach(this::saveJob);
        notifyJobStatsOnChangeListeners();
        return jobs;
    }

    @Override
    public List<Job> getJobs(StateName state, Instant updatedBefore, PageRequest pageRequest) {
        return jobQueue.values().stream()
                .filter(job -> job.hasState(state) && job.getUpdatedAt().isBefore(updatedBefore))
                .skip(pageRequest.getOffset())
                .limit(pageRequest.getLimit())
                .map(this::deepClone)
                .collect(toList());
    }

    @Override
    public List<Job> getScheduledJobs(Instant scheduledBefore, PageRequest pageRequest) {
        return getJobs(SCHEDULED)
                .filter(job -> ((ScheduledState) job.getJobState()).getScheduledAt().isBefore(scheduledBefore))
                .skip(pageRequest.getOffset())
                .limit(pageRequest.getLimit())
                .map(this::deepClone)
                .collect(toList());
    }

    @Override
    public Long countJobs(StateName state) {
        return getJobs(state).count();
    }

    @Override
    public List<Job> getJobs(StateName state, PageRequest pageRequest) {
        Comparator<Job> comparator = getJobComparator(pageRequest);
        return getJobs(state)
                .skip(pageRequest.getOffset())
                .limit(pageRequest.getLimit())
                .sorted(comparator)
                .map(this::deepClone)
                .collect(toList());
    }

    private Comparator<Job> getJobComparator(PageRequest pageRequest) {
        Comparator<Job> comparator = null;
        if (pageRequest.getOrderField().equals("createdAt")) {
            comparator = Comparator.comparing(Job::getCreatedAt);
        } else if (pageRequest.getOrderField().equals("updatedAt")) {
            comparator = Comparator.comparing(Job::getUpdatedAt);
        }
        if (comparator != null && pageRequest.getOrder() == PageRequest.Order.DESC) comparator = comparator.reversed();
        return comparator;
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
        notifyJobStatsOnChangeListeners();
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
        deleteRecurringJob(recurringJob.getId());
        recurringJobs.add(recurringJob);
        return recurringJob;
    }

    @Override
    public List<RecurringJob> getRecurringJobs() {
        return recurringJobs;
    }

    @Override
    public int deleteRecurringJob(String id) {
        recurringJobs.removeIf(job -> id.equals(job.getId()));
        return 0;
    }

    @Override
    public JobStats getJobStats() {
        return new JobStats(
                (long) jobQueue.size(),
                getJobs(AWAITING).count(),
                getJobs(SCHEDULED).count(),
                getJobs(ENQUEUED).count(),
                getJobs(PROCESSING).count(),
                getJobs(FAILED).count(),
                getJobs(SUCCEEDED).count() + jobStats.getOrDefault(SUCCEEDED, new AtomicLong()).get(),
                getJobs(DELETED).count(),
                recurringJobs.size(),
                backgroundJobServers.size()
        );
    }

    @Override
    public void publishJobStatCounter(StateName state, int amount) {
        jobStats.put(state, new AtomicLong(amount));
    }

    private Stream<Job> getJobs(StateName state) {
        return jobQueue.values().stream()
                .filter(job -> job.hasState(state))
                .sorted(comparing(Job::getCreatedAt));
    }

    private Job deepClone(Job job) {
        final String serializedJobAsString = jobMapper.serializeJob(job);
        final Job result = jobMapper.deserializeJob(serializedJobAsString);
        setFieldUsingAutoboxing("locker", result, getValueFromFieldOrProperty(job, "locker"));
        return result;
    }

    private void saveJob(Job job) {
        if (job.getId() == null) {
            job.setId(UUID.randomUUID());
        } else {
            final Job oldJob = jobQueue.get(job.getId());
            if (oldJob != null && job.getVersion() != oldJob.getVersion()) {
                throw new ConcurrentJobModificationException(job);
            }
            job.increaseVersion();
        }

        jobQueue.put(job.getId(), deepClone(job));
    }
}
