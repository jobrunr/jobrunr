package org.jobrunr.storage.nosql.redis;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.*;
import org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers;
import org.jobrunr.utils.annotations.Beta;
import org.jobrunr.utils.resilience.RateLimiter;
import redis.clients.jedis.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

import static java.time.Instant.now;
import static java.util.Arrays.stream;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.jobrunr.jobs.states.StateName.AWAITING;
import static org.jobrunr.jobs.states.StateName.DELETED;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.FAILED;
import static org.jobrunr.jobs.states.StateName.PROCESSING;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;
import static org.jobrunr.storage.StorageProviderUtils.areNewJobs;
import static org.jobrunr.storage.StorageProviderUtils.notAllJobsAreExisting;
import static org.jobrunr.storage.StorageProviderUtils.notAllJobsAreNew;
import static org.jobrunr.storage.nosql.redis.RedisUtilities.backgroundJobServerKey;
import static org.jobrunr.storage.nosql.redis.RedisUtilities.jobCounterKey;
import static org.jobrunr.storage.nosql.redis.RedisUtilities.jobDetailsKey;
import static org.jobrunr.storage.nosql.redis.RedisUtilities.jobKey;
import static org.jobrunr.storage.nosql.redis.RedisUtilities.jobQueueForStateKey;
import static org.jobrunr.storage.nosql.redis.RedisUtilities.jobVersionKey;
import static org.jobrunr.storage.nosql.redis.RedisUtilities.recurringJobKey;
import static org.jobrunr.storage.nosql.redis.RedisUtilities.toMicroSeconds;
import static org.jobrunr.utils.JobUtils.getJobSignature;
import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;
import static org.jobrunr.utils.resilience.RateLimiter.SECOND;

@Beta
public class JedisRedisStorageProvider extends AbstractStorageProvider {

    public static final String RECURRING_JOBS_KEY = "recurringjobs";
    public static final String BACKGROUND_JOB_SERVERS_KEY = "backgroundjobservers";
    public static final String QUEUE_SCHEDULEDJOBS_KEY = "queue:scheduledjobs";

    private final JedisPool jedisPool;
    private JobMapper jobMapper;

    public JedisRedisStorageProvider() {
        this(new JedisPool());
    }

    public JedisRedisStorageProvider(JedisPool jedisPool) {
        this(jedisPool, rateLimit().at1Request().per(SECOND));
    }

    public JedisRedisStorageProvider(JedisPool jedisPool, RateLimiter changeListenerNotificationRateLimit) {
        super(changeListenerNotificationRateLimit);
        this.jedisPool = jedisPool;
    }

    @Override
    public void setJobMapper(JobMapper jobMapper) {
        this.jobMapper = jobMapper;
    }

    @Override
    public void announceBackgroundJobServer(BackgroundJobServerStatus serverStatus) {
        try (final Jedis jedis = getJedis(); final Pipeline p = jedis.pipelined()) {
            p.hset(backgroundJobServerKey(serverStatus), BackgroundJobServers.FIELD_ID, serverStatus.getId().toString());
            p.hset(backgroundJobServerKey(serverStatus), BackgroundJobServers.FIELD_WORKER_POOL_SIZE, String.valueOf(serverStatus.getWorkerPoolSize()));
            p.hset(backgroundJobServerKey(serverStatus), BackgroundJobServers.FIELD_POLL_INTERVAL_IN_SECONDS, String.valueOf(serverStatus.getPollIntervalInSeconds()));
            p.hset(backgroundJobServerKey(serverStatus), BackgroundJobServers.FIELD_DELETE_SUCCEEDED_JOBS_AFTER, String.valueOf(serverStatus.getDeleteSucceededJobsAfter()));
            p.hset(backgroundJobServerKey(serverStatus), BackgroundJobServers.FIELD_DELETE_DELETED_JOBS_AFTER, String.valueOf(serverStatus.getPermanentlyDeleteDeletedJobsAfter()));
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
        try (final Jedis jedis = getJedis()) {
            final Map<String, String> valueMap = jedis.hgetAll(backgroundJobServerKey(serverStatus));
            if (valueMap.isEmpty())
                throw new ServerTimedOutException(serverStatus, new StorageException("BackgroundJobServer with id " + serverStatus.getId() + " was not found"));
            try (final Pipeline p = jedis.pipelined()) {
                p.watch(backgroundJobServerKey(serverStatus));
                p.hset(backgroundJobServerKey(serverStatus), BackgroundJobServers.FIELD_LAST_HEARTBEAT, String.valueOf(serverStatus.getLastHeartbeat()));
                p.hset(backgroundJobServerKey(serverStatus), BackgroundJobServers.FIELD_SYSTEM_FREE_MEMORY, String.valueOf(serverStatus.getSystemFreeMemory()));
                p.hset(backgroundJobServerKey(serverStatus), BackgroundJobServers.FIELD_SYSTEM_CPU_LOAD, String.valueOf(serverStatus.getSystemCpuLoad()));
                p.hset(backgroundJobServerKey(serverStatus), BackgroundJobServers.FIELD_PROCESS_FREE_MEMORY, String.valueOf(serverStatus.getProcessFreeMemory()));
                p.hset(backgroundJobServerKey(serverStatus), BackgroundJobServers.FIELD_PROCESS_ALLOCATED_MEMORY, String.valueOf(serverStatus.getProcessAllocatedMemory()));
                p.hset(backgroundJobServerKey(serverStatus), BackgroundJobServers.FIELD_PROCESS_CPU_LOAD, String.valueOf(serverStatus.getProcessCpuLoad()));
                p.zadd(BACKGROUND_JOB_SERVERS_KEY, toMicroSeconds(now()), serverStatus.getId().toString());
                final Response<String> isRunningResponse = p.hget(backgroundJobServerKey(serverStatus), BackgroundJobServers.FIELD_IS_RUNNING);
                p.sync();
                return Boolean.parseBoolean(isRunningResponse.get());
            }
        }
    }

    @Override
    public void signalBackgroundJobServerStopped(BackgroundJobServerStatus serverStatus) {
        try (final Jedis jedis = getJedis(); final Pipeline p = jedis.pipelined()) {
            p.del(backgroundJobServerKey(serverStatus.getId()));
            p.zrem(BACKGROUND_JOB_SERVERS_KEY, serverStatus.getId().toString());
            p.sync();
        }
    }

    @Override
    public List<BackgroundJobServerStatus> getBackgroundJobServers() {
        try (final Jedis jedis = getJedis()) {
            return new JedisRedisPipelinedStream<>(jedis.zrange(BACKGROUND_JOB_SERVERS_KEY, 0, Integer.MAX_VALUE), jedis)
                    .mapUsingPipeline((p, id) -> p.hgetAll(backgroundJobServerKey(id)))
                    .mapAfterSync(Response::get)
                    .map(fieldMap -> new BackgroundJobServerStatus(
                            UUID.fromString(fieldMap.get(BackgroundJobServers.FIELD_ID)),
                            Integer.parseInt(fieldMap.get(BackgroundJobServers.FIELD_WORKER_POOL_SIZE)),
                            Integer.parseInt(fieldMap.get(BackgroundJobServers.FIELD_POLL_INTERVAL_IN_SECONDS)),
                            Duration.parse(fieldMap.get(BackgroundJobServers.FIELD_DELETE_SUCCEEDED_JOBS_AFTER)),
                            Duration.parse(fieldMap.get(BackgroundJobServers.FIELD_DELETE_DELETED_JOBS_AFTER)),
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
                    .sorted(comparing(BackgroundJobServerStatus::getFirstHeartbeat))
                    .collect(toList());
        }
    }

    @Override
    public int removeTimedOutBackgroundJobServers(Instant heartbeatOlderThan) {
        try (final Jedis jedis = getJedis()) {
            final Set<String> backgroundjobservers = jedis.zrangeByScore(BACKGROUND_JOB_SERVERS_KEY, 0, toMicroSeconds(heartbeatOlderThan));
            try (final Pipeline p = jedis.pipelined()) {
                backgroundjobservers.forEach(backgroundJobServerId -> {
                    p.del(backgroundJobServerKey(backgroundJobServerId));
                    p.zrem(BACKGROUND_JOB_SERVERS_KEY, backgroundJobServerId);
                });
                p.sync();
            }
            return backgroundjobservers.size();
        }
    }

    @Override
    public Job save(Job jobToSave) {
        try (final Jedis jedis = getJedis()) {
            boolean isNewJob = jobToSave.getId() == null;
            if (isNewJob) {
                insertJob(jobToSave, jedis);
            } else {
                updateJob(jobToSave, jedis);
            }
            notifyJobStatsOnChangeListeners();
        }
        return jobToSave;
    }

    @Override
    public int deletePermanently(UUID id) {
        Job job = getJobById(id);
        try (final Jedis jedis = getJedis(); Transaction transaction = jedis.multi()) {
            transaction.del(jobKey(job));
            transaction.del(jobVersionKey(job));
            deleteJobMetadata(transaction, job);
            final List<Object> result = transaction.exec();
            int amount = result == null || result.isEmpty() ? 0 : 1;
            notifyJobStatsOnChangeListenersIf(amount > 0);
            return amount;
        }
    }

    @Override
    public Job getJobById(UUID id) {
        try (final Jedis jedis = getJedis()) {
            final String serializedJobAsString = jedis.get(jobKey(id));
            if (serializedJobAsString == null) throw new JobNotFoundException(id);

            return jobMapper.deserializeJob(serializedJobAsString);
        }
    }

    @Override
    public List<Job> save(List<Job> jobs) {
        if (jobs.isEmpty()) return jobs;

        if (areNewJobs(jobs)) {
            if (notAllJobsAreNew(jobs)) {
                throw new IllegalArgumentException("All jobs must be either new (with id == null) or existing (with id != null)");
            }
            try (final Jedis jedis = getJedis(); Transaction p = jedis.multi()) {
                for (Job jobToSave : jobs) {
                    jobToSave.setId(UUID.randomUUID());
                    saveJob(p, jobToSave);
                }
                p.exec();
            }
        } else {
            if (notAllJobsAreExisting(jobs)) {
                throw new IllegalArgumentException("All jobs must be either new (with id == null) or existing (with id != null)");
            }
            try (final Jedis jedis = getJedis()) {
                for (Job job : jobs) {
                    updateJob(job, jedis);
                }
            }
        }
        notifyJobStatsOnChangeListenersIf(!jobs.isEmpty());
        return jobs;
    }

    @Override
    public List<Job> getJobs(StateName state, Instant updatedBefore, PageRequest pageRequest) {
        try (final Jedis jedis = getJedis()) {
            Set<String> jobsByState;
            if ("updatedAt:ASC".equals(pageRequest.getOrder())) {
                jobsByState = jedis.zrangeByScore(jobQueueForStateKey(state), 0, toMicroSeconds(updatedBefore), (int) pageRequest.getOffset(), pageRequest.getLimit());
            } else if ("updatedAt:DESC".equals(pageRequest.getOrder())) {
                jobsByState = jedis.zrevrangeByScore(jobQueueForStateKey(state), toMicroSeconds(updatedBefore), 0, (int) pageRequest.getOffset(), pageRequest.getLimit());
            } else {
                throw new IllegalArgumentException("Unsupported sorting: " + pageRequest.getOrder());
            }
            return new JedisRedisPipelinedStream<>(jobsByState, jedis)
                    .mapUsingPipeline((p, id) -> p.get(jobKey(id)))
                    .mapAfterSync(Response::get)
                    .map(jobMapper::deserializeJob)
                    .collect(toList());
        }
    }

    @Override
    public List<Job> getScheduledJobs(Instant scheduledBefore, PageRequest pageRequest) {
        try (final Jedis jedis = getJedis()) {
            return new JedisRedisPipelinedStream<>(jedis.zrangeByScore(QUEUE_SCHEDULEDJOBS_KEY, 0, toMicroSeconds(now()), (int) pageRequest.getOffset(), pageRequest.getLimit()), jedis)
                    .mapUsingPipeline((p, id) -> p.get(jobKey(id)))
                    .mapAfterSync(Response::get)
                    .map(jobMapper::deserializeJob)
                    .collect(toList());
        }
    }

    @Override
    public Long countJobs(StateName state) {
        try (final Jedis jedis = getJedis()) {
            return jedis.zcount(jobQueueForStateKey(state), 0, Long.MAX_VALUE);
        }
    }

    @Override
    public List<Job> getJobs(StateName state, PageRequest pageRequest) {
        try (final Jedis jedis = getJedis()) {
            Set<String> jobsByState;
            // we only support what is used by frontend
            if ("updatedAt:ASC".equals(pageRequest.getOrder())) {
                jobsByState = jedis.zrange(jobQueueForStateKey(state), pageRequest.getOffset(), pageRequest.getOffset() + pageRequest.getLimit() - 1);
            } else if ("updatedAt:DESC".equals(pageRequest.getOrder())) {
                jobsByState = jedis.zrevrange(jobQueueForStateKey(state), pageRequest.getOffset(), pageRequest.getOffset() + pageRequest.getLimit() - 1);
            } else {
                throw new IllegalArgumentException("Unsupported sorting: " + pageRequest.getOrder());
            }
            return new JedisRedisPipelinedStream<>(jobsByState, jedis)
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
    public int deleteJobsPermanently(StateName state, Instant updatedBefore) {
        int amount = 0;
        try (final Jedis jedis = getJedis()) {
            Set<String> zrangeToInspect = jedis.zrange(jobQueueForStateKey(state), 0, 1000);
            outerloop:
            while (!zrangeToInspect.isEmpty()) {
                for (String id : zrangeToInspect) {
                    final Job job = getJobById(UUID.fromString(id));
                    if (job.getUpdatedAt().isAfter(updatedBefore)) break outerloop;

                    try (Transaction transaction = jedis.multi()) {
                        transaction.del(jobKey(job));
                        transaction.del(jobVersionKey(job));
                        deleteJobMetadata(transaction, job);

                        final List<Object> exec = transaction.exec();
                        if (exec != null && !exec.isEmpty()) amount++;
                    }
                }
                zrangeToInspect = jedis.zrange(jobQueueForStateKey(state), 0, 1000);
            }
        }
        notifyJobStatsOnChangeListenersIf(amount > 0);
        return amount;
    }

    @Override
    public Set<String> getDistinctJobSignatures(StateName... states) {
        try (final Jedis jedis = getJedis(); Pipeline p = jedis.pipelined()) {
            List<Response<Set<String>>> jobSignatures = stream(states)
                    .map(stateName -> p.smembers(jobDetailsKey(stateName)))
                    .collect(toList());
            p.sync();
            return jobSignatures.stream().flatMap(res -> res.get().stream()).collect(toSet());
        }
    }

    @Override
    public boolean exists(JobDetails jobDetails, StateName... states) {
        try (final Jedis jedis = getJedis(); Pipeline p = jedis.pipelined()) {
            List<Response<Boolean>> existsJob = stream(states)
                    .map(stateName -> p.sismember(jobDetailsKey(stateName), getJobSignature(jobDetails)))
                    .collect(toList());
            p.sync();
            return existsJob.stream().map(Response::get).filter(b -> b).findAny().orElse(false);
        }
    }

    @Override
    public boolean recurringJobExists(String recurringJobId, StateName... states) {
        try (final Jedis jedis = getJedis(); Pipeline p = jedis.pipelined()) {
            List<Response<Boolean>> existsJob = stream(states)
                    .map(stateName -> p.sismember(recurringJobKey(stateName), recurringJobId))
                    .collect(toList());
            p.sync();
            return existsJob.stream().map(Response::get).filter(b -> b).findAny().orElse(false);
        }
    }

    @Override
    public RecurringJob saveRecurringJob(RecurringJob recurringJob) {
        try (final Jedis jedis = getJedis(); Pipeline p = jedis.pipelined()) {
            p.set(recurringJobKey(recurringJob.getId()), jobMapper.serializeRecurringJob(recurringJob));
            p.sadd(RECURRING_JOBS_KEY, recurringJob.getId());
            p.sync();
        }
        return recurringJob;
    }

    @Override
    public List<RecurringJob> getRecurringJobs() {
        try (final Jedis jedis = getJedis()) {
            return jedis.smembers(RECURRING_JOBS_KEY)
                    .stream()
                    .map(id -> jedis.get("recurringjob:" + id))
                    .map(jobMapper::deserializeRecurringJob)
                    .collect(toList());
        }
    }

    @Override
    public int deleteRecurringJob(String id) {
        try (final Jedis jedis = getJedis(); final Transaction transaction = jedis.multi()) {
            transaction.del("recurringjob:" + id);
            transaction.srem(RECURRING_JOBS_KEY, id);

            final List<Object> exec = transaction.exec();
            return (exec != null && !exec.isEmpty()) ? 1 : 0;
        }
    }

    @Override
    public JobStats getJobStats() {
        Instant instant = Instant.now();
        try (final Jedis jedis = getJedis(); final Pipeline p = jedis.pipelined()) {
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
            final Response<String> deletedCounterResponse = p.get(jobCounterKey(DELETED));
            final Response<Long> deletedResponse = p.zcount(jobQueueForStateKey(DELETED), 0, Long.MAX_VALUE);

            final Response<Long> recurringJobsResponse = p.scard(RECURRING_JOBS_KEY);
            final Response<Long> backgroundJobServerResponse = p.zcount(BACKGROUND_JOB_SERVERS_KEY, 0, Long.MAX_VALUE);

            p.sync();

            final Long awaitingCount = getCounterValue(waitingCounterResponse, waitingResponse);
            final Long scheduledCount = getCounterValue(scheduledCounterResponse, scheduledResponse);
            final Long enqueuedCount = getCounterValue(enqueuedCounterResponse, enqueuedResponse);
            final Long processingCount = getCounterValue(processingCounterResponse, processingResponse);
            final Long succeededCount = getCounterValue(succeededCounterResponse, succeededResponse);
            final Long failedCount = getCounterValue(failedCounterResponse, failedResponse);
            final Long deletedCount = getCounterValue(deletedCounterResponse, deletedResponse);
            final Long total = scheduledCount + enqueuedCount + processingCount + succeededResponse.get() + failedCount;
            final Long recurringJobsCount = recurringJobsResponse.get();
            final Long backgroundJobServerCount = backgroundJobServerResponse.get();
            return new JobStats(
                    instant,
                    total,
                    awaitingCount,
                    scheduledCount,
                    enqueuedCount,
                    processingCount,
                    failedCount,
                    succeededCount,
                    deletedCount,
                    recurringJobsCount.intValue(),
                    backgroundJobServerCount.intValue()
            );
        }
    }

    @Override
    public void publishJobStatCounter(StateName state, int amount) {
        try (final Jedis jedis = getJedis()) {
            jedis.incrBy(jobCounterKey(state), amount);
        }
    }

    protected Jedis getJedis() {
        return jedisPool.getResource();
    }

    private void insertJob(Job jobToSave, Jedis jedis) {
        jobToSave.setId(UUID.randomUUID());
        try (Transaction transaction = jedis.multi()) {
            saveJob(transaction, jobToSave);
            transaction.exec();
        }
    }

    private void updateJob(Job jobToSave, Jedis jedis) {
        jedis.watch(jobVersionKey(jobToSave));
        final int version = Integer.parseInt(jedis.get(jobVersionKey(jobToSave)));
        if (version != jobToSave.getVersion()) throw new ConcurrentJobModificationException(jobToSave);
        jobToSave.increaseVersion();
        try (Transaction transaction = jedis.multi()) {
            saveJob(transaction, jobToSave);
            List<Object> result = transaction.exec();
            jedis.unwatch();
            if (result == null || result.isEmpty()) throw new ConcurrentJobModificationException(jobToSave);
        }
    }

    private void saveJob(Transaction transaction, Job jobToSave) {
        deleteJobMetadataForUpdate(transaction, jobToSave);
        transaction.set(jobVersionKey(jobToSave), String.valueOf(jobToSave.getVersion()));
        transaction.set(jobKey(jobToSave), jobMapper.serializeJob(jobToSave));
        transaction.zadd(jobQueueForStateKey(jobToSave.getState()), toMicroSeconds(jobToSave.getUpdatedAt()), jobToSave.getId().toString());
        transaction.sadd(jobDetailsKey(jobToSave.getState()), getJobSignature(jobToSave.getJobDetails()));
        if (SCHEDULED.equals(jobToSave.getState())) {
            transaction.zadd(QUEUE_SCHEDULEDJOBS_KEY, toMicroSeconds(((ScheduledState) jobToSave.getJobState()).getScheduledAt()), jobToSave.getId().toString());
        }
        jobToSave.getJobStatesOfType(ScheduledState.class).findFirst().map(ScheduledState::getRecurringJobId).ifPresent(recurringJobId -> transaction.sadd(recurringJobKey(jobToSave.getState()), recurringJobId));
    }

    private void deleteJobMetadataForUpdate(Transaction transaction, Job job) {
        String id = job.getId().toString();
        transaction.zrem(QUEUE_SCHEDULEDJOBS_KEY, id);
        Stream.of(StateName.values()).forEach(stateName -> transaction.zrem(jobQueueForStateKey(stateName), id));
        Stream.of(StateName.values()).filter(stateName -> !SCHEDULED.equals(stateName)).forEach(stateName -> transaction.srem(jobDetailsKey(stateName), getJobSignature(job.getJobDetails())));
        if ((job.hasState(ENQUEUED) && job.getJobStates().size() >= 2 && job.getJobState(-2) instanceof ScheduledState)
                || (job.hasState(DELETED) && job.getJobStates().size() >= 2 && job.getJobState(-2) instanceof ScheduledState)) {
            transaction.srem(jobDetailsKey(SCHEDULED), getJobSignature(job.getJobDetails()));
        }
        job.getJobStatesOfType(ScheduledState.class).findFirst().map(ScheduledState::getRecurringJobId).ifPresent(recurringJobId -> Stream.of(StateName.values()).forEach(stateName -> transaction.srem(recurringJobKey(stateName), recurringJobId)));
    }

    private void deleteJobMetadata(Transaction transaction, Job job) {
        String id = job.getId().toString();
        transaction.zrem(QUEUE_SCHEDULEDJOBS_KEY, id);
        Stream.of(StateName.values()).forEach(stateName -> transaction.zrem(jobQueueForStateKey(stateName), id));
        Stream.of(StateName.values()).forEach(stateName -> transaction.srem(jobDetailsKey(stateName), getJobSignature(job.getJobDetails())));
    }

    private long getCounterValue(Response<String> counterResponse, Response<Long> countResponse) {
        return countResponse.get() + Long.parseLong(counterResponse.get() != null ? counterResponse.get() : "0");
    }
}