package org.jobrunr.storage.nosql.redis;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.AbstractStorageProvider;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.jobrunr.storage.JobNotFoundException;
import org.jobrunr.storage.JobStats;
import org.jobrunr.storage.Page;
import org.jobrunr.storage.PageRequest;
import org.jobrunr.storage.ServerTimedOutException;
import org.jobrunr.storage.StorageException;
import org.jobrunr.storage.StorageProviderConstants.BackgroundJobServers;
import org.jobrunr.utils.annotations.Beta;
import org.jobrunr.utils.resilience.RateLimiter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.commands.RedisPipeline;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.time.Instant.now;
import static java.util.stream.Collectors.toList;
import static org.jobrunr.jobs.states.StateName.AWAITING;
import static org.jobrunr.jobs.states.StateName.DELETED;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.FAILED;
import static org.jobrunr.jobs.states.StateName.PROCESSING;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;
import static org.jobrunr.utils.JobUtils.getJobSignature;
import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;
import static org.jobrunr.utils.resilience.RateLimiter.SECOND;

@Beta
public class RedisStorageProvider extends AbstractStorageProvider {

    public static final String RECURRING_JOBS_KEY = "recurringjobs";
    public static final String BACKGROUND_JOB_SERVERS_KEY = "backgroundjobservers";
    public static final String QUEUE_SCHEDULEDJOBS_KEY = "queue:scheduledjobs";

    private final Jedis jedisConnector;
    private JobMapper jobMapper;

    public RedisStorageProvider() {
        this(new Jedis());
    }

    public RedisStorageProvider(Jedis jedis) {
        this(jedis, rateLimit().at2Requests().per(SECOND));
    }

    public RedisStorageProvider(Jedis jedis, RateLimiter changeListenerNotificationRateLimit) {
        super(changeListenerNotificationRateLimit);
        this.jedisConnector = jedis;
    }

    @Override
    public void setJobMapper(JobMapper jobMapper) {
        this.jobMapper = jobMapper;
    }

    @Override
    public void announceBackgroundJobServer(BackgroundJobServerStatus serverStatus) {
        try (Jedis jedis = getJedis()) {
            final Pipeline p = jedis.pipelined();
            p.hset(backgroundJobServerKey(serverStatus), BackgroundJobServers.FIELD_ID, serverStatus.getId().toString());
            p.hset(backgroundJobServerKey(serverStatus), BackgroundJobServers.FIELD_WORKER_POOL_SIZE, String.valueOf(serverStatus.getWorkerPoolSize()));
            p.hset(backgroundJobServerKey(serverStatus), BackgroundJobServers.FIELD_POLL_INTERVAL_IN_SECONDS, String.valueOf(serverStatus.getPollIntervalInSeconds()));
            p.hset(backgroundJobServerKey(serverStatus), BackgroundJobServers.FIELD_FIRST_HEARTBEAT, String.valueOf(serverStatus.getFirstHeartbeat()));
            p.hset(backgroundJobServerKey(serverStatus), BackgroundJobServers.FIELD_LAST_HEARTBEAT, String.valueOf(serverStatus.getLastHeartbeat()));
            p.hset(backgroundJobServerKey(serverStatus), BackgroundJobServers.FIELD_IS_RUNNING, String.valueOf(serverStatus.isRunning()));
            p.hset(backgroundJobServerKey(serverStatus), BackgroundJobServers.FIELD_SYSTEM_TOTAL_MEMORY, String.valueOf(serverStatus.getSystemTotalMemory()));
            p.hset(backgroundJobServerKey(serverStatus), BackgroundJobServers.FIELD_SYSTEM_FREE_MEMORY, String.valueOf(serverStatus.getSystemFreeMemory()));
            p.hset(backgroundJobServerKey(serverStatus), BackgroundJobServers.FIELD_SYSTEM_CPU_LOAD, String.valueOf(serverStatus.getSystemCpuLoad()));
            p.hset(backgroundJobServerKey(serverStatus), BackgroundJobServers.FIELD_PROCESS_MAX_MEMORY, String.valueOf(serverStatus.getProcessMaxMemory()));
            p.hset(backgroundJobServerKey(serverStatus), BackgroundJobServers.FIELD_PROCESS_FREE_MEMORY, String.valueOf(serverStatus.getProcessFreeMemory()));
            p.hset(backgroundJobServerKey(serverStatus), BackgroundJobServers.FIELD_PROCESS_ALLOCATED_MEMORY, String.valueOf(serverStatus.getProcessAllocatedMemory()));
            p.hset(backgroundJobServerKey(serverStatus), BackgroundJobServers.FIELD_PROCESS_CPU_LOAD, String.valueOf(serverStatus.getProcessCpuLoad()));
            p.zadd(BACKGROUND_JOB_SERVERS_KEY, toMicroSeconds(now()), serverStatus.getId().toString());
            p.sync();
        }
    }

    @Override
    public boolean signalBackgroundJobServerAlive(BackgroundJobServerStatus serverStatus) {
        try (Jedis jedis = getJedis()) {
            final Map<String, String> valueMap = jedis.hgetAll(backgroundJobServerKey(serverStatus));
            if (valueMap.isEmpty()) throw new ServerTimedOutException(serverStatus, new StorageException("BackgroundJobServer with id " + serverStatus.getId() + " was not found"));
            final Pipeline p = jedis.pipelined();
            p.watch(backgroundJobServerKey(serverStatus));
            p.hset(backgroundJobServerKey(serverStatus), BackgroundJobServers.FIELD_LAST_HEARTBEAT, String.valueOf(serverStatus.getLastHeartbeat()));
            p.hset(backgroundJobServerKey(serverStatus), BackgroundJobServers.FIELD_SYSTEM_FREE_MEMORY, String.valueOf(serverStatus.getSystemFreeMemory()));
            p.hset(backgroundJobServerKey(serverStatus), BackgroundJobServers.FIELD_SYSTEM_CPU_LOAD, String.valueOf(serverStatus.getSystemCpuLoad()));
            p.hset(backgroundJobServerKey(serverStatus), BackgroundJobServers.FIELD_PROCESS_FREE_MEMORY, String.valueOf(serverStatus.getProcessFreeMemory()));
            p.hset(backgroundJobServerKey(serverStatus), BackgroundJobServers.FIELD_PROCESS_ALLOCATED_MEMORY, String.valueOf(serverStatus.getProcessAllocatedMemory()));
            p.hset(backgroundJobServerKey(serverStatus), BackgroundJobServers.FIELD_PROCESS_CPU_LOAD, String.valueOf(serverStatus.getProcessCpuLoad()));
            final Response<String> isRunningResponse = p.hget(backgroundJobServerKey(serverStatus), BackgroundJobServers.FIELD_IS_RUNNING);
            p.sync();
            return Boolean.parseBoolean(isRunningResponse.get());
        }
    }

    @Override
    public List<BackgroundJobServerStatus> getBackgroundJobServers() {
        try (Jedis jedis = getJedis()) {
            return new RedisPipelinedStream<>(jedis.zrange(BACKGROUND_JOB_SERVERS_KEY, 0, Integer.MAX_VALUE), jedis)
                    .mapUsingPipeline((p, id) -> p.hgetAll(backgroundJobServerKey(id)))
                    .mapAfterSync(Response::get)
                    .map(fieldMap -> new BackgroundJobServerStatus(
                            UUID.fromString(fieldMap.get(BackgroundJobServers.FIELD_ID)),
                            Integer.parseInt(fieldMap.get(BackgroundJobServers.FIELD_WORKER_POOL_SIZE)),
                            Integer.parseInt(fieldMap.get(BackgroundJobServers.FIELD_POLL_INTERVAL_IN_SECONDS)),
                            Instant.parse(fieldMap.get(BackgroundJobServers.FIELD_FIRST_HEARTBEAT)),
                            Instant.parse(fieldMap.get(BackgroundJobServers.FIELD_LAST_HEARTBEAT)),
                            Boolean.parseBoolean(fieldMap.get(BackgroundJobServers.FIELD_IS_RUNNING)),
                            Long.parseLong(fieldMap.get(BackgroundJobServers.FIELD_SYSTEM_TOTAL_MEMORY)),
                            Long.parseLong(fieldMap.get(BackgroundJobServers.FIELD_SYSTEM_FREE_MEMORY)),
                            Double.parseDouble(fieldMap.get(BackgroundJobServers.FIELD_SYSTEM_CPU_LOAD)),
                            Long.parseLong(fieldMap.get(BackgroundJobServers.FIELD_PROCESS_MAX_MEMORY)),
                            Long.parseLong(fieldMap.get(BackgroundJobServers.FIELD_PROCESS_FREE_MEMORY)),
                            Long.parseLong(fieldMap.get(BackgroundJobServers.FIELD_PROCESS_ALLOCATED_MEMORY)),
                            Double.parseDouble(fieldMap.get(BackgroundJobServers.FIELD_PROCESS_CPU_LOAD))
                    ))
                    .collect(Collectors.toList());
        }
    }

    @Override
    public int removeTimedOutBackgroundJobServers(Instant heartbeatOlderThan) {
        try (Jedis jedis = getJedis()) {
            final Set<String> backgroundjobservers = jedis.zrangeByScore(BACKGROUND_JOB_SERVERS_KEY, 0, toMicroSeconds(heartbeatOlderThan));
            final Pipeline p = jedis.pipelined();

            backgroundjobservers.forEach(backgroundJobServerId -> {
                p.del(backgroundJobServerKey(backgroundJobServerId));
                p.zrem(BACKGROUND_JOB_SERVERS_KEY, backgroundJobServerId);
            });
            p.sync();
            return backgroundjobservers.size();
        }
    }

    @Override
    public Job save(Job jobToSave) {
        try (Jedis jedis = getJedis()) {
            boolean isNewJob = jobToSave.getId() == null;
            if (isNewJob) {
                insertJob(jobToSave, jedis);
            } else {
                updateJob(jobToSave, jedis);
            }
            notifyOnChangeListeners();
        }
        return jobToSave;
    }

    @Override
    public int delete(UUID id) {
        Job job = getJobById(id);
        try (Jedis jedis = getJedis()) {
            Transaction transaction = jedis.multi();
            transaction.del(jobKey(job));
            transaction.del(jobVersionKey(job));
            deleteJobMetadata(transaction, job);
            final List<Object> result = transaction.exec();
            int amount = result == null || result.isEmpty() ? 0 : 1;
            notifyOnChangeListenersIf(amount > 0);
            return amount;
        }
    }

    @Override
    public Job getJobById(UUID id) {
        try (Jedis jedis = getJedis()) {
            final String serializedJobAsString = jedis.get(jobKey(id));
            if (serializedJobAsString == null) throw new JobNotFoundException(id);

            return jobMapper.deserializeJob(serializedJobAsString);
        }
    }

    @Override
    public List<Job> save(List<Job> jobs) {
        if (areNewJobs(jobs)) {
            if (notAllJobsAreNew(jobs)) {
                throw new IllegalArgumentException("All jobs must be either new (with id == null) or existing (with id != null)");
            }
            try (Jedis jedis = getJedis()) {
                Pipeline p = jedis.pipelined();
                jobs.forEach(jobToSave -> {
                    jobToSave.setId(UUID.randomUUID());
                    saveJob(p, jobToSave);
                    p.publish("job-queue-channel", jobToSave.getId().toString());
                });
                p.sync();
            }
        } else {
            if (notAllJobsAreExisting(jobs)) {
                throw new IllegalArgumentException("All jobs must be either new (with id == null) or existing (with id != null)");
            }
            try (Jedis jedis = getJedis()) {
                jobs.forEach(job -> updateJob(job, jedis));
            }
        }
        notifyOnChangeListenersIf(jobs.size() > 0);
        return jobs;
    }

    @Override
    public List<Job> getJobs(StateName state, Instant updatedBefore, PageRequest pageRequest) {
        try (Jedis jedis = getJedis()) {
            Set<String> jobsByState;
            if (PageRequest.Order.ASC == pageRequest.getOrder()) {
                jobsByState = jedis.zrangeByScore(jobQueueForStateKey(state), 0, toMicroSeconds(updatedBefore));
            } else {
                jobsByState = jedis.zrevrangeByScore(jobQueueForStateKey(state), 0, toMicroSeconds(updatedBefore));
            }
            return new RedisPipelinedStream<>(jobsByState, jedis)
                    .skip(pageRequest.getOffset())
                    .limit(pageRequest.getLimit())
                    .mapUsingPipeline((p, id) -> p.get(jobKey(id)))
                    .mapAfterSync(Response::get)
                    .map(jobMapper::deserializeJob)
                    .collect(toList());
        }
    }

    @Override
    public List<Job> getScheduledJobs(Instant scheduledBefore, PageRequest pageRequest) {
        return new RedisPipelinedStream<>(jedisConnector.zrangeByScore(QUEUE_SCHEDULEDJOBS_KEY, 0, toMicroSeconds(now())), jedisConnector)
                .skip(pageRequest.getOffset())
                .limit(pageRequest.getLimit())
                .mapUsingPipeline((p, id) -> p.get(jobKey(id)))
                .mapAfterSync(Response::get)
                .map(jobMapper::deserializeJob)
                .collect(toList());
    }

    @Override
    public Long countJobs(StateName state) {
        try (Jedis jedis = getJedis()) {
            return jedis.zcount(jobQueueForStateKey(state), 0, Long.MAX_VALUE);
        }
    }

    @Override
    public List<Job> getJobs(StateName state, PageRequest pageRequest) {
        try (Jedis jedis = getJedis()) {
            Set<String> jobsByState;
            if (PageRequest.Order.ASC == pageRequest.getOrder()) {
                jobsByState = jedis.zrange(jobQueueForStateKey(state), pageRequest.getOffset(), pageRequest.getOffset() + pageRequest.getLimit() - 1);
            } else {
                jobsByState = jedis.zrevrange(jobQueueForStateKey(state), pageRequest.getOffset(), pageRequest.getOffset() + pageRequest.getLimit() - 1);
            }
            return new RedisPipelinedStream<>(jobsByState, jedis)
                    .mapUsingPipeline((p, id) -> p.get(jobKey(id)))
                    .mapAfterSync(Response::get)
                    .map(jobMapper::deserializeJob)
                    .collect(toList());
        }
    }

    @Override
    public Page<Job> getJobPage(StateName state, PageRequest pageRequest) {
        long count = countJobs(state);
        if (count > 0) {
            List<Job> jobs = getJobs(state, pageRequest);
            return new Page<>(count, jobs, pageRequest);
        }
        return new Page<>(0, new ArrayList<>(), pageRequest);
    }

    @Override
    public int deleteJobs(StateName state, Instant updatedBefore) {
        int amount = 0;
        try (Jedis jedis = getJedis()) {
            Set<String> zrangeToInspect = jedis.zrange(jobQueueForStateKey(state), 0, 1000);
            outerloop:
            while (!zrangeToInspect.isEmpty()) {
                for (String id : zrangeToInspect) {
                    final Job job = getJobById(UUID.fromString(id));
                    if (job.getUpdatedAt().isAfter(updatedBefore)) break outerloop;

                    Transaction transaction = jedis.multi();
                    transaction.del(jobKey(job));
                    transaction.del(jobVersionKey(job));
                    deleteJobMetadata(transaction, job);

                    final List<Object> exec = transaction.exec();
                    if (exec != null && exec.size() > 0) amount++;
                }
                zrangeToInspect = jedis.zrange(jobQueueForStateKey(state), 0, 1000);
            }
        }
        notifyOnChangeListenersIf(amount > 0);
        return amount;
    }

    @Override
    public boolean exists(JobDetails jobDetails, StateName state) {
        try (Jedis jedis = getJedis()) {
            return jedis.sismember(jobDetailsKey(state), getJobSignature(jobDetails));
        }
    }

    @Override
    public RecurringJob saveRecurringJob(RecurringJob recurringJob) {
        try (Jedis jedis = getJedis()) {
            final Pipeline p = jedis.pipelined();
            p.set(recurringJobKey(recurringJob.getId()), jobMapper.serializeRecurringJob(recurringJob));
            p.sadd(RECURRING_JOBS_KEY, recurringJob.getId());
            p.sync();
        }
        return recurringJob;
    }

    @Override
    public List<RecurringJob> getRecurringJobs() {
        try (Jedis jedis = getJedis()) {
            return jedis.smembers(RECURRING_JOBS_KEY)
                    .stream()
                    .map(id -> jedis.get("recurringjob:" + id))
                    .map(jobMapper::deserializeRecurringJob)
                    .collect(toList());
        }
    }

    @Override
    public int deleteRecurringJob(String id) {
        try (Jedis jedis = getJedis()) {
            final Transaction transaction = jedis.multi();
            transaction.del("recurringjob:" + id);
            transaction.srem(RECURRING_JOBS_KEY, id);

            final List<Object> exec = transaction.exec();
            return (exec != null && exec.size() > 0) ? 1 : 0;
        }
    }

    @Override
    public JobStats getJobStats() {
        try (Jedis jedis = getJedis()) {
            final Pipeline p = jedis.pipelined();
            final Response<String> waitingCounterResponse = p.get(jobCounterKey(AWAITING));
            final Response<Long> waitingResponse = p.zcount(jobQueueForStateKey(AWAITING), 0, Long.MAX_VALUE);
            final Response<String> scheduledCounterResponse = p.get(jobCounterKey(SCHEDULED));
            final Response<Long> scheduledResponse = p.zcount(jobQueueForStateKey(SCHEDULED), 0, Long.MAX_VALUE);
            final Response<String> enqueuedCounterResponse = p.get(jobCounterKey(ENQUEUED));
            final Response<Long> enqueuedResponse = p.zcount(jobQueueForStateKey(ENQUEUED), 0, Long.MAX_VALUE);
            final Response<String> processingCounterResponse = p.get(jobCounterKey(PROCESSING));
            final Response<Long> processingResponse = p.zcount(jobQueueForStateKey(PROCESSING), 0, Long.MAX_VALUE);
            final Response<String> succeededCounterResponse = p.get(jobCounterKey(SUCCEEDED));
            final Response<Long> succeededResponse = p.zcount(jobQueueForStateKey(SUCCEEDED), 0, Long.MAX_VALUE);
            final Response<String> failedCounterResponse = p.get(jobCounterKey(FAILED));
            final Response<Long> failedResponse = p.zcount(jobQueueForStateKey(FAILED), 0, Long.MAX_VALUE);

            final Response<Long> recurringJobsResponse = p.scard(RECURRING_JOBS_KEY);
            final Response<Long> backgroundJobServerResponse = p.zcount(BACKGROUND_JOB_SERVERS_KEY, 0, Long.MAX_VALUE);

            p.sync();

            final Long awaitingCount = getCounterValue(waitingCounterResponse, waitingResponse);
            final Long scheduledCount = getCounterValue(scheduledCounterResponse, scheduledResponse);
            final Long enqueuedCount = getCounterValue(enqueuedCounterResponse, enqueuedResponse);
            final Long processingCount = getCounterValue(processingCounterResponse, processingResponse);
            final Long succeededCount = getCounterValue(succeededCounterResponse, succeededResponse);
            final Long failedCount = getCounterValue(failedCounterResponse, failedResponse);
            final Long total = scheduledCount + enqueuedCount + processingCount + succeededResponse.get() + failedCount;
            final Long recurringJobsCount = recurringJobsResponse.get();
            final Long backgroundJobServerCount = backgroundJobServerResponse.get();
            return new JobStats(
                    total,
                    awaitingCount,
                    scheduledCount,
                    enqueuedCount,
                    processingCount,
                    failedCount,
                    succeededCount,
                    recurringJobsCount.intValue(),
                    backgroundJobServerCount.intValue()
            );
        }
    }

    @Override
    public void publishJobStatCounter(StateName state, int amount) {
        try (Jedis jedis = getJedis()) {
            jedis.incrBy(jobCounterKey(state), amount);
        }
    }

    protected Jedis getJedis() {
        return jedisConnector;
    }

    private boolean notAllJobsAreNew(List<Job> jobs) {
        return jobs.stream().anyMatch(job -> job.getId() != null);
    }

    private boolean notAllJobsAreExisting(List<Job> jobs) {
        return jobs.stream().anyMatch(job -> job.getId() == null);
    }

    private boolean areNewJobs(List<Job> jobs) {
        return jobs.get(0).getId() == null;
    }

    private void insertJob(Job jobToSave, Jedis jedis) {
        jobToSave.setId(UUID.randomUUID());
        Pipeline p = jedis.pipelined();
        saveJob(p, jobToSave);
        p.publish("job-queue-channel", jobToSave.getId().toString());
        p.sync();
    }

    private void updateJob(Job jobToSave, Jedis jedis) {
        jedis.watch(jobVersionKey(jobToSave));
        final int version = Integer.parseInt(jedis.get(jobVersionKey(jobToSave)));
        if (version != jobToSave.getVersion()) throw new ConcurrentJobModificationException(jobToSave.getId());
        jobToSave.increaseVersion();
        Transaction transaction = jedis.multi();
        saveJob(transaction, jobToSave);
        List<Object> result = transaction.exec();
        jedis.unwatch();
        if (result == null || result.isEmpty()) throw new ConcurrentJobModificationException(jobToSave.getId());
    }

    private void saveJob(RedisPipeline p, Job jobToSave) {
        deleteJobMetadata(p, jobToSave);
        p.set(jobVersionKey(jobToSave), String.valueOf(jobToSave.getVersion()));
        p.set(jobKey(jobToSave), jobMapper.serializeJob(jobToSave));
        p.zadd(jobQueueForStateKey(jobToSave.getState()), toMicroSeconds(jobToSave.getUpdatedAt()), jobToSave.getId().toString());
        p.sadd(jobDetailsKey(jobToSave.getState()), getJobSignature(jobToSave.getJobDetails()));
        if (SCHEDULED.equals(jobToSave.getState())) {
            p.zadd(QUEUE_SCHEDULEDJOBS_KEY, toMicroSeconds(((ScheduledState) jobToSave.getJobState()).getScheduledAt()), jobToSave.getId().toString());
        }
    }

    private void deleteJobMetadata(RedisPipeline p, Job job) {
        String id = job.getId().toString();
        p.zrem(jobQueueForStateKey(AWAITING), id);
        p.zrem(jobQueueForStateKey(SCHEDULED), id);
        p.zrem(jobQueueForStateKey(ENQUEUED), id);
        p.zrem(jobQueueForStateKey(PROCESSING), id);
        p.zrem(jobQueueForStateKey(FAILED), id);
        p.zrem(jobQueueForStateKey(SUCCEEDED), id);
        p.zrem(jobQueueForStateKey(DELETED), id);
        p.zrem(QUEUE_SCHEDULEDJOBS_KEY, id);

        p.srem(jobDetailsKey(AWAITING), getJobSignature(job.getJobDetails()));
        p.srem(jobDetailsKey(SCHEDULED), getJobSignature(job.getJobDetails()));
        p.srem(jobDetailsKey(ENQUEUED), getJobSignature(job.getJobDetails()));
        p.srem(jobDetailsKey(PROCESSING), getJobSignature(job.getJobDetails()));
        p.srem(jobDetailsKey(FAILED), getJobSignature(job.getJobDetails()));
        p.srem(jobDetailsKey(SUCCEEDED), getJobSignature(job.getJobDetails()));
        p.srem(jobDetailsKey(DELETED), getJobSignature(job.getJobDetails()));
    }

    private long getCounterValue(Response<String> waitingCounterResponse, Response<Long> waitingResponse) {
        return waitingResponse.get() + Long.parseLong(waitingCounterResponse.get() != null ? waitingCounterResponse.get() : "0");
    }

    private String jobCounterKey(StateName stateName) {
        return "counter:jobs:" + stateName;
    }

    private String backgroundJobServerKey(BackgroundJobServerStatus serverStatus) {
        return backgroundJobServerKey(serverStatus.getId());
    }

    private String backgroundJobServerKey(UUID serverId) {
        return "backgroundjobserver:" + serverId.toString();
    }

    private String backgroundJobServerKey(String serverId) {
        return "backgroundjobserver:" + serverId;
    }

    private String jobQueueForStateKey(StateName stateName) {
        return "queue:jobs:" + stateName;
    }

    private String recurringJobKey(String id) {
        return "recurringjob:" + id;
    }

    private String jobKey(Job job) {
        return jobKey(job.getId());
    }

    private String jobKey(UUID id) {
        return jobKey(id.toString());
    }

    private String jobKey(String id) {
        return "job:" + id;
    }

    private String jobDetailsKey(StateName stateName) {
        return "job:jobdetails:" + stateName;
    }

    private String jobVersionKey(Job job) {
        return jobVersionKey(job.getId());
    }

    private String jobVersionKey(UUID id) {
        return jobKey(id) + ":version";
    }

    private long toMicroSeconds(Instant instant) {
        return ChronoUnit.MICROS.between(Instant.EPOCH, instant);
    }
}
