package org.jobrunr.storage.nosql.redis;

import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.support.ConnectionPoolSupport;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.*;
import org.jobrunr.utils.annotations.Beta;
import org.jobrunr.utils.resilience.RateLimiter;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

import static io.lettuce.core.Range.unbounded;
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
@SuppressWarnings("unchecked")
public class LettuceRedisStorageProvider extends AbstractStorageProvider {

    public static final String RECURRING_JOBS_KEY = "recurringjobs";
    public static final String BACKGROUND_JOB_SERVERS_KEY = "backgroundjobservers";
    public static final String QUEUE_SCHEDULEDJOBS_KEY = "queue:scheduledjobs";

    private final RedisClient redisClient;
    private final GenericObjectPool<StatefulRedisConnection> pool;
    private JobMapper jobMapper;

    public LettuceRedisStorageProvider(RedisClient redisClient) {
        this(redisClient, rateLimit().at1Request().per(SECOND));
    }

    public LettuceRedisStorageProvider(RedisClient redisClient, RateLimiter changeListenerNotificationRateLimit) {
        super(changeListenerNotificationRateLimit);
        this.redisClient = redisClient;
        pool = ConnectionPoolSupport.createGenericObjectPool(this::createConnection, new GenericObjectPoolConfig());
    }

    @Override
    public void setJobMapper(JobMapper jobMapper) {
        this.jobMapper = jobMapper;
    }

    @Override
    public void announceBackgroundJobServer(BackgroundJobServerStatus serverStatus) {
        try (final StatefulRedisConnection connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            commands.multi();
            commands.hset(backgroundJobServerKey(serverStatus), StorageProviderUtils.BackgroundJobServers.FIELD_ID, serverStatus.getId().toString());
            commands.hset(backgroundJobServerKey(serverStatus), StorageProviderUtils.BackgroundJobServers.FIELD_WORKER_POOL_SIZE, String.valueOf(serverStatus.getWorkerPoolSize()));
            commands.hset(backgroundJobServerKey(serverStatus), StorageProviderUtils.BackgroundJobServers.FIELD_POLL_INTERVAL_IN_SECONDS, String.valueOf(serverStatus.getPollIntervalInSeconds()));
            commands.hset(backgroundJobServerKey(serverStatus), StorageProviderUtils.BackgroundJobServers.FIELD_DELETE_SUCCEEDED_JOBS_AFTER, String.valueOf(serverStatus.getDeleteSucceededJobsAfter()));
            commands.hset(backgroundJobServerKey(serverStatus), StorageProviderUtils.BackgroundJobServers.FIELD_DELETE_DELETED_JOBS_AFTER, String.valueOf(serverStatus.getPermanentlyDeleteDeletedJobsAfter()));
            commands.hset(backgroundJobServerKey(serverStatus), StorageProviderUtils.BackgroundJobServers.FIELD_FIRST_HEARTBEAT, String.valueOf(serverStatus.getFirstHeartbeat()));
            commands.hset(backgroundJobServerKey(serverStatus), StorageProviderUtils.BackgroundJobServers.FIELD_LAST_HEARTBEAT, String.valueOf(serverStatus.getLastHeartbeat()));
            commands.hset(backgroundJobServerKey(serverStatus), StorageProviderUtils.BackgroundJobServers.FIELD_IS_RUNNING, String.valueOf(serverStatus.isRunning()));
            commands.hset(backgroundJobServerKey(serverStatus), StorageProviderUtils.BackgroundJobServers.FIELD_SYSTEM_TOTAL_MEMORY, String.valueOf(serverStatus.getSystemTotalMemory()));
            commands.hset(backgroundJobServerKey(serverStatus), StorageProviderUtils.BackgroundJobServers.FIELD_SYSTEM_FREE_MEMORY, String.valueOf(serverStatus.getSystemFreeMemory()));
            commands.hset(backgroundJobServerKey(serverStatus), StorageProviderUtils.BackgroundJobServers.FIELD_SYSTEM_CPU_LOAD, String.valueOf(serverStatus.getSystemCpuLoad()));
            commands.hset(backgroundJobServerKey(serverStatus), StorageProviderUtils.BackgroundJobServers.FIELD_PROCESS_MAX_MEMORY, String.valueOf(serverStatus.getProcessMaxMemory()));
            commands.hset(backgroundJobServerKey(serverStatus), StorageProviderUtils.BackgroundJobServers.FIELD_PROCESS_FREE_MEMORY, String.valueOf(serverStatus.getProcessFreeMemory()));
            commands.hset(backgroundJobServerKey(serverStatus), StorageProviderUtils.BackgroundJobServers.FIELD_PROCESS_ALLOCATED_MEMORY, String.valueOf(serverStatus.getProcessAllocatedMemory()));
            commands.hset(backgroundJobServerKey(serverStatus), StorageProviderUtils.BackgroundJobServers.FIELD_PROCESS_CPU_LOAD, String.valueOf(serverStatus.getProcessCpuLoad()));
            commands.zadd(BACKGROUND_JOB_SERVERS_KEY, toMicroSeconds(now()), serverStatus.getId().toString());
            commands.exec();
        }
    }

    @Override
    public boolean signalBackgroundJobServerAlive(BackgroundJobServerStatus serverStatus) {
        try (final StatefulRedisConnection connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            final Map<String, String> valueMap = commands.hgetall(backgroundJobServerKey(serverStatus));
            if (valueMap.isEmpty())
                throw new ServerTimedOutException(serverStatus, new StorageException("BackgroundJobServer with id " + serverStatus.getId() + " was not found"));

            commands.watch(backgroundJobServerKey(serverStatus));
            commands.multi();
            commands.hset(backgroundJobServerKey(serverStatus), StorageProviderUtils.BackgroundJobServers.FIELD_LAST_HEARTBEAT, String.valueOf(serverStatus.getLastHeartbeat()));
            commands.hset(backgroundJobServerKey(serverStatus), StorageProviderUtils.BackgroundJobServers.FIELD_SYSTEM_FREE_MEMORY, String.valueOf(serverStatus.getSystemFreeMemory()));
            commands.hset(backgroundJobServerKey(serverStatus), StorageProviderUtils.BackgroundJobServers.FIELD_SYSTEM_CPU_LOAD, String.valueOf(serverStatus.getSystemCpuLoad()));
            commands.hset(backgroundJobServerKey(serverStatus), StorageProviderUtils.BackgroundJobServers.FIELD_PROCESS_FREE_MEMORY, String.valueOf(serverStatus.getProcessFreeMemory()));
            commands.hset(backgroundJobServerKey(serverStatus), StorageProviderUtils.BackgroundJobServers.FIELD_PROCESS_ALLOCATED_MEMORY, String.valueOf(serverStatus.getProcessAllocatedMemory()));
            commands.hset(backgroundJobServerKey(serverStatus), StorageProviderUtils.BackgroundJobServers.FIELD_PROCESS_CPU_LOAD, String.valueOf(serverStatus.getProcessCpuLoad()));
            commands.zadd(BACKGROUND_JOB_SERVERS_KEY, toMicroSeconds(now()), serverStatus.getId().toString());
            commands.exec();
            commands.unwatch();
            return Boolean.parseBoolean(commands.hget(backgroundJobServerKey(serverStatus), StorageProviderUtils.BackgroundJobServers.FIELD_IS_RUNNING));
        }
    }

    @Override
    public void signalBackgroundJobServerStopped(BackgroundJobServerStatus serverStatus) {
        try (final StatefulRedisConnection connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            commands.multi();
            commands.del(backgroundJobServerKey(serverStatus.getId()));
            commands.zrem(BACKGROUND_JOB_SERVERS_KEY, serverStatus.getId().toString());
            commands.exec();
        }
    }

    @Override
    public List<BackgroundJobServerStatus> getBackgroundJobServers() {
        try (final StatefulRedisConnection connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            List<String> zrange = commands.zrange(BACKGROUND_JOB_SERVERS_KEY, 0, Integer.MAX_VALUE);
            return new LettuceRedisPipelinedStream<>(zrange, connection)
                    .mapUsingPipeline((p, id) -> p.hgetall(backgroundJobServerKey(id)))
                    .mapToValues()
                    .map(fieldMap -> new BackgroundJobServerStatus(
                            UUID.fromString(fieldMap.get(StorageProviderUtils.BackgroundJobServers.FIELD_ID)),
                            Integer.parseInt(fieldMap.get(StorageProviderUtils.BackgroundJobServers.FIELD_WORKER_POOL_SIZE)),
                            Integer.parseInt(fieldMap.get(StorageProviderUtils.BackgroundJobServers.FIELD_POLL_INTERVAL_IN_SECONDS)),
                            Duration.parse(fieldMap.get(StorageProviderUtils.BackgroundJobServers.FIELD_DELETE_SUCCEEDED_JOBS_AFTER)),
                            Duration.parse(fieldMap.get(StorageProviderUtils.BackgroundJobServers.FIELD_DELETE_DELETED_JOBS_AFTER)),
                            Instant.parse(fieldMap.get(StorageProviderUtils.BackgroundJobServers.FIELD_FIRST_HEARTBEAT)),
                            Instant.parse(fieldMap.get(StorageProviderUtils.BackgroundJobServers.FIELD_LAST_HEARTBEAT)),
                            Boolean.parseBoolean(fieldMap.get(StorageProviderUtils.BackgroundJobServers.FIELD_IS_RUNNING)),
                            Long.parseLong(fieldMap.get(StorageProviderUtils.BackgroundJobServers.FIELD_SYSTEM_TOTAL_MEMORY)),
                            Long.parseLong(fieldMap.get(StorageProviderUtils.BackgroundJobServers.FIELD_SYSTEM_FREE_MEMORY)),
                            Double.parseDouble(fieldMap.get(StorageProviderUtils.BackgroundJobServers.FIELD_SYSTEM_CPU_LOAD)),
                            Long.parseLong(fieldMap.get(StorageProviderUtils.BackgroundJobServers.FIELD_PROCESS_MAX_MEMORY)),
                            Long.parseLong(fieldMap.get(StorageProviderUtils.BackgroundJobServers.FIELD_PROCESS_FREE_MEMORY)),
                            Long.parseLong(fieldMap.get(StorageProviderUtils.BackgroundJobServers.FIELD_PROCESS_ALLOCATED_MEMORY)),
                            Double.parseDouble(fieldMap.get(StorageProviderUtils.BackgroundJobServers.FIELD_PROCESS_CPU_LOAD))
                    ))
                    .sorted(comparing(BackgroundJobServerStatus::getFirstHeartbeat))
                    .collect(toList());
        }
    }

    @Override
    public int removeTimedOutBackgroundJobServers(Instant heartbeatOlderThan) {
        try (final StatefulRedisConnection connection = getConnection()) {
            RedisCommands commands = connection.sync();
            final List<String> backgroundJobServers = commands.zrangebyscore(BACKGROUND_JOB_SERVERS_KEY, Range.create(0, toMicroSeconds(heartbeatOlderThan)));
            commands.multi();
            backgroundJobServers.forEach(backgroundJobServerId -> {
                commands.del(backgroundJobServerKey(backgroundJobServerId));
                commands.zrem(BACKGROUND_JOB_SERVERS_KEY, backgroundJobServerId);
            });
            commands.exec();
            return backgroundJobServers.size();
        }
    }

    @Override
    public Job save(Job jobToSave) {
        try (final StatefulRedisConnection connection = getConnection()) {
            RedisCommands commands = connection.sync();
            boolean isNewJob = jobToSave.getId() == null;
            if (isNewJob) {
                insertJob(jobToSave, commands);
            } else {
                updateJob(jobToSave, commands);
            }
            notifyJobStatsOnChangeListeners();
        }
        return jobToSave;
    }

    @Override
    public int deletePermanently(UUID id) {
        Job job = getJobById(id);
        try (final StatefulRedisConnection connection = getConnection()) {
            RedisCommands commands = connection.sync();
            commands.multi();
            commands.del(jobKey(job));
            commands.del(jobVersionKey(job));
            deleteJobMetadata(commands, job);
            final TransactionResult result = commands.exec();
            int amount = result == null || result.isEmpty() ? 0 : 1;
            notifyJobStatsOnChangeListenersIf(amount > 0);
            return amount;
        }
    }

    @Override
    public Job getJobById(UUID id) {
        try (final StatefulRedisConnection connection = getConnection()) {
            RedisCommands commands = connection.sync();
            final Object serializedJob = commands.get(jobKey(id));
            if (serializedJob == null) throw new JobNotFoundException(id);
            return jobMapper.deserializeJob(serializedJob.toString());
        }
    }

    @Override
    public List<Job> save(List<Job> jobs) {
        if (jobs.isEmpty()) return jobs;

        if (areNewJobs(jobs)) {
            if (notAllJobsAreNew(jobs)) {
                throw new IllegalArgumentException("All jobs must be either new (with id == null) or existing (with id != null)");
            }
            try (final StatefulRedisConnection connection = getConnection()) {
                RedisCommands commands = connection.sync();
                commands.multi();
                for (Job jobToSave : jobs) {
                    jobToSave.setId(UUID.randomUUID());
                    saveJob(commands, jobToSave);
                }
                commands.exec();
            }
        } else {
            if (notAllJobsAreExisting(jobs)) {
                throw new IllegalArgumentException("All jobs must be either new (with id == null) or existing (with id != null)");
            }
            try (final StatefulRedisConnection connection = getConnection()) {
                RedisCommands commands = connection.sync();
                for (Job job : jobs) {
                    updateJob(job, commands);
                }
            }
        }
        notifyJobStatsOnChangeListenersIf(!jobs.isEmpty());
        return jobs;
    }

    @Override
    public List<Job> getJobs(StateName state, Instant updatedBefore, PageRequest pageRequest) {
        try (final StatefulRedisConnection connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            List<String> jobsByState;
            if ("updatedAt:ASC".equals(pageRequest.getOrder())) {
                jobsByState = commands.zrangebyscore(jobQueueForStateKey(state), Range.create(0, toMicroSeconds(updatedBefore)), Limit.create(pageRequest.getOffset(), pageRequest.getLimit()));
            } else if ("updatedAt:DESC".equals(pageRequest.getOrder())) {
                jobsByState = commands.zrevrangebyscore(jobQueueForStateKey(state), Range.create(0, toMicroSeconds(updatedBefore)), Limit.create(pageRequest.getOffset(), pageRequest.getLimit()));
            } else {
                throw new IllegalArgumentException("Unsupported sorting: " + pageRequest.getOrder());
            }
            return new LettuceRedisPipelinedStream<>(jobsByState, connection)
                    .mapUsingPipeline((p, id) -> p.get(jobKey(id)))
                    .mapAfterSync(RedisFuture<String>::get)
                    .map(jobMapper::deserializeJob)
                    .collect(toList());
        }
    }

    @Override
    public List<Job> getScheduledJobs(Instant scheduledBefore, PageRequest pageRequest) {
        try (final StatefulRedisConnection connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            return new LettuceRedisPipelinedStream<>(commands.zrangebyscore(QUEUE_SCHEDULEDJOBS_KEY, Range.create(0, toMicroSeconds(now())), Limit.create(pageRequest.getOffset(), pageRequest.getLimit())), connection)
                    .mapUsingPipeline((p, id) -> p.get(jobKey(id)))
                    .mapAfterSync(RedisFuture<String>::get)
                    .map(jobMapper::deserializeJob)
                    .collect(toList());
        }
    }

    @Override
    public Long countJobs(StateName state) {
        try (final StatefulRedisConnection connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            return commands.zcount(jobQueueForStateKey(state), unbounded());
        }
    }

    @Override
    public List<Job> getJobs(StateName state, PageRequest pageRequest) {
        try (final StatefulRedisConnection connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            List<String> jobsByState;
            // we only support what is used by frontend
            if ("updatedAt:ASC".equals(pageRequest.getOrder())) {
                jobsByState = commands.zrange(jobQueueForStateKey(state), pageRequest.getOffset(), pageRequest.getOffset() + pageRequest.getLimit() - 1);
            } else if ("updatedAt:DESC".equals(pageRequest.getOrder())) {
                jobsByState = commands.zrevrange(jobQueueForStateKey(state), pageRequest.getOffset(), pageRequest.getOffset() + pageRequest.getLimit() - 1);
            } else {
                throw new IllegalArgumentException("Unsupported sorting: " + pageRequest.getOrder());
            }
            return new LettuceRedisPipelinedStream<>(jobsByState, connection)
                    .mapUsingPipeline((p, id) -> p.get(jobKey(id)))
                    .mapAfterSync(RedisFuture<String>::get)
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
        try (final StatefulRedisConnection connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            List<String> zrangeToInspect = commands.zrange(jobQueueForStateKey(state), 0, 1000);
            outerloop:
            while (!zrangeToInspect.isEmpty()) {
                for (String id : zrangeToInspect) {
                    final Job job = getJobById(UUID.fromString(id));
                    if (job.getUpdatedAt().isAfter(updatedBefore)) break outerloop;

                    commands.multi();
                    commands.del(jobKey(job));
                    commands.del(jobVersionKey(job));
                    deleteJobMetadata(commands, job);

                    final TransactionResult exec = commands.exec();
                    if (exec != null && !exec.isEmpty()) amount++;
                }
                zrangeToInspect = commands.zrange(jobQueueForStateKey(state), 0, 1000);
            }
        }

        notifyJobStatsOnChangeListenersIf(amount > 0);
        return amount;
    }

    @Override
    public Set<String> getDistinctJobSignatures(StateName... states) {
        try (final StatefulRedisConnection connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            List<Set<String>> jobSignatures = stream(states)
                    .map(stateName -> commands.smembers(jobDetailsKey(stateName)))
                    .collect(toList());
            return jobSignatures.stream().flatMap(Collection::stream).collect(toSet());
        }
    }

    @Override
    public boolean exists(JobDetails jobDetails, StateName... states) {
        try (final StatefulRedisConnection connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            List<Boolean> existsJob = stream(states)
                    .map(stateName -> commands.sismember(jobDetailsKey(stateName), getJobSignature(jobDetails)))
                    .collect(toList());
            return existsJob.stream().filter(b -> b).findAny().orElse(false);
        }
    }

    @Override
    public boolean recurringJobExists(String recurringJobId, StateName... states) {
        try (final StatefulRedisConnection connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            List<Boolean> existsJob = stream(states)
                    .map(stateName -> commands.sismember(recurringJobKey(stateName), recurringJobId))
                    .collect(toList());
            return existsJob.stream().filter(b -> b).findAny().orElse(false);
        }
    }

    @Override
    public RecurringJob saveRecurringJob(RecurringJob recurringJob) {
        try (final StatefulRedisConnection connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            commands.multi();
            commands.set(recurringJobKey(recurringJob.getId()), jobMapper.serializeRecurringJob(recurringJob));
            commands.sadd(RECURRING_JOBS_KEY, recurringJob.getId());
            commands.exec();
        }
        return recurringJob;
    }

    @Override
    public List<RecurringJob> getRecurringJobs() {
        try (final StatefulRedisConnection connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            return commands.smembers(RECURRING_JOBS_KEY)
                    .stream()
                    .map(id -> commands.get("recurringjob:" + id))
                    .map(jobMapper::deserializeRecurringJob)
                    .collect(toList());
        }
    }

    @Override
    public int deleteRecurringJob(String id) {
        try (final StatefulRedisConnection connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            commands.multi();
            commands.del("recurringjob:" + id);
            commands.srem(RECURRING_JOBS_KEY, id);

            final TransactionResult exec = commands.exec();
            return (exec != null && !exec.isEmpty()) ? 1 : 0;
        }
    }

    @Override
    public JobStats getJobStats() {
        Instant instant = Instant.now();
        try (final StatefulRedisConnection connection = getConnection()) {
            connection.setAutoFlushCommands(false);
            RedisAsyncCommands<String, String> commands = connection.async();
            final RedisFuture<String> waitingCounterResponse = commands.get(jobCounterKey(AWAITING));
            final RedisFuture<Long> waitingResponse = commands.zcount(jobQueueForStateKey(AWAITING), unbounded());
            final RedisFuture<String> scheduledCounterResponse = commands.get(jobCounterKey(SCHEDULED));
            final RedisFuture<Long> scheduledResponse = commands.zcount(jobQueueForStateKey(SCHEDULED), unbounded());
            final RedisFuture<String> enqueuedCounterResponse = commands.get(jobCounterKey(ENQUEUED));
            final RedisFuture<Long> enqueuedResponse = commands.zcount(jobQueueForStateKey(ENQUEUED), unbounded());
            final RedisFuture<String> processingCounterResponse = commands.get(jobCounterKey(PROCESSING));
            final RedisFuture<Long> processingResponse = commands.zcount(jobQueueForStateKey(PROCESSING), unbounded());
            final RedisFuture<String> succeededCounterResponse = commands.get(jobCounterKey(SUCCEEDED));
            final RedisFuture<Long> succeededResponse = commands.zcount(jobQueueForStateKey(SUCCEEDED), unbounded());
            final RedisFuture<String> failedCounterResponse = commands.get(jobCounterKey(FAILED));
            final RedisFuture<Long> failedResponse = commands.zcount(jobQueueForStateKey(FAILED), unbounded());
            final RedisFuture<String> deletedCounterResponse = commands.get(jobCounterKey(DELETED));
            final RedisFuture<Long> deletedResponse = commands.zcount(jobQueueForStateKey(DELETED), unbounded());

            final RedisFuture<Long> recurringJobsResponse = commands.scard(RECURRING_JOBS_KEY);
            final RedisFuture<Long> backgroundJobServerResponse = commands.zcount(BACKGROUND_JOB_SERVERS_KEY, unbounded());

            connection.flushCommands();
            LettuceFutures.awaitAll(Duration.ofSeconds(10), waitingCounterResponse, waitingResponse, scheduledCounterResponse, scheduledResponse,
                    enqueuedCounterResponse, enqueuedResponse, processingCounterResponse, processingResponse, succeededCounterResponse, succeededResponse,
                    failedCounterResponse, failedResponse, deletedCounterResponse, deletedResponse);

            final Long awaitingCount = getCounterValue(waitingCounterResponse, waitingResponse);
            final Long scheduledCount = getCounterValue(scheduledCounterResponse, scheduledResponse);
            final Long enqueuedCount = getCounterValue(enqueuedCounterResponse, enqueuedResponse);
            final Long processingCount = getCounterValue(processingCounterResponse, processingResponse);
            final Long succeededCount = getCounterValue(succeededCounterResponse, succeededResponse);
            final Long failedCount = getCounterValue(failedCounterResponse, failedResponse);
            final Long deletedCount = getCounterValue(deletedCounterResponse, deletedResponse);
            final Long total = scheduledCount + enqueuedCount + processingCount + succeededCount + failedCount;
            final Long recurringJobsCount = getCounterValue(null, recurringJobsResponse);
            final Long backgroundJobServerCount = getCounterValue(null, backgroundJobServerResponse);
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
        try (final StatefulRedisConnection connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            commands.incrby(jobCounterKey(state), amount);
        }
    }

    @Override
    public void close() {
        super.close();
        pool.close();
    }

    private void insertJob(Job jobToSave, RedisCommands commands) {
        jobToSave.setId(UUID.randomUUID());
        commands.multi();
        saveJob(commands, jobToSave);
        commands.exec();
    }

    private void updateJob(Job jobToSave, RedisCommands commands) {
        commands.watch(jobVersionKey(jobToSave));
        final int version = Integer.parseInt(commands.get(jobVersionKey(jobToSave)).toString());
        if (version != jobToSave.getVersion()) throw new ConcurrentJobModificationException(jobToSave);
        jobToSave.increaseVersion();
        commands.multi();
        saveJob(commands, jobToSave);
        TransactionResult result = commands.exec();
        commands.unwatch();
        if (result == null || result.isEmpty()) throw new ConcurrentJobModificationException(jobToSave);
    }

    private void saveJob(RedisCommands commands, Job jobToSave) {
        deleteJobMetadataForUpdate(commands, jobToSave);
        commands.set(jobVersionKey(jobToSave), String.valueOf(jobToSave.getVersion()));
        commands.set(jobKey(jobToSave), jobMapper.serializeJob(jobToSave));
        commands.zadd(jobQueueForStateKey(jobToSave.getState()), toMicroSeconds(jobToSave.getUpdatedAt()), jobToSave.getId().toString());
        commands.sadd(jobDetailsKey(jobToSave.getState()), getJobSignature(jobToSave.getJobDetails()));
        if (SCHEDULED.equals(jobToSave.getState())) {
            commands.zadd(QUEUE_SCHEDULEDJOBS_KEY, toMicroSeconds(((ScheduledState) jobToSave.getJobState()).getScheduledAt()), jobToSave.getId().toString());
        }
        jobToSave.getJobStatesOfType(ScheduledState.class).findFirst().map(ScheduledState::getRecurringJobId).ifPresent(recurringJobId -> commands.sadd(recurringJobKey(jobToSave.getState()), recurringJobId));
    }

    private void deleteJobMetadataForUpdate(RedisCommands commands, Job job) {
        String id = job.getId().toString();
        commands.zrem(QUEUE_SCHEDULEDJOBS_KEY, id);
        Stream.of(StateName.values()).forEach(stateName -> commands.zrem(jobQueueForStateKey(stateName), id));
        Stream.of(StateName.values()).filter(stateName -> !SCHEDULED.equals(stateName)).forEach(stateName -> commands.srem(jobDetailsKey(stateName), getJobSignature(job.getJobDetails())));
        if ((job.hasState(ENQUEUED) && job.getJobStates().size() >= 2 && job.getJobState(-2) instanceof ScheduledState)
                || (job.hasState(DELETED) && job.getJobStates().size() >= 2 && job.getJobState(-2) instanceof ScheduledState)) {
            commands.srem(jobDetailsKey(SCHEDULED), getJobSignature(job.getJobDetails()));
        }
        job.getJobStatesOfType(ScheduledState.class).findFirst().map(ScheduledState::getRecurringJobId).ifPresent(recurringJobId -> Stream.of(StateName.values()).forEach(stateName -> commands.srem(recurringJobKey(stateName), recurringJobId)));
    }

    private void deleteJobMetadata(RedisCommands commands, Job job) {
        String id = job.getId().toString();
        commands.zrem(QUEUE_SCHEDULEDJOBS_KEY, id);
        Stream.of(StateName.values()).forEach(stateName -> commands.zrem(jobQueueForStateKey(stateName), id));
        Stream.of(StateName.values()).forEach(stateName -> commands.srem(jobDetailsKey(stateName), getJobSignature(job.getJobDetails())));
    }

    private long getCounterValue(RedisFuture<String> counterResponse, RedisFuture<Long> countResponse) {
        try {
            Long count = countResponse.get();
            if (counterResponse != null) {
                count += Long.parseLong(counterResponse.get() != null ? counterResponse.get() : "0");
            }
            return count;
        } catch (Exception e) {
            return 0L;
        }
    }

    protected StatefulRedisConnection getConnection() {
        try {
            StatefulRedisConnection statefulRedisConnection = pool.borrowObject();
            statefulRedisConnection.setAutoFlushCommands(true);
            return statefulRedisConnection;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    StatefulRedisConnection createConnection() {
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        return connection;
    }
}
