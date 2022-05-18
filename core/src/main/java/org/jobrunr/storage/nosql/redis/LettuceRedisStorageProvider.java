package org.jobrunr.storage.nosql.redis;

import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.support.ConnectionPoolSupport;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.jobrunr.jobs.*;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.JobStats;
import org.jobrunr.storage.*;
import org.jobrunr.storage.nosql.NoSqlStorageProvider;
import org.jobrunr.utils.annotations.Beta;
import org.jobrunr.utils.resilience.RateLimiter;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.lettuce.core.Range.unbounded;
import static java.time.Instant.now;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.jobrunr.jobs.states.StateName.*;
import static org.jobrunr.storage.JobRunrMetadata.toId;
import static org.jobrunr.storage.StorageProviderUtils.*;
import static org.jobrunr.storage.nosql.redis.RedisUtilities.*;
import static org.jobrunr.utils.JobUtils.getJobSignature;
import static org.jobrunr.utils.NumberUtils.parseLong;
import static org.jobrunr.utils.StringUtils.isNullOrEmpty;
import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;
import static org.jobrunr.utils.resilience.RateLimiter.SECOND;

@Beta
public class LettuceRedisStorageProvider extends AbstractStorageProvider implements NoSqlStorageProvider {

    private final ObjectPool<StatefulRedisConnection<String, String>> pool;
    private final String keyPrefix;
    private JobMapper jobMapper;

    public LettuceRedisStorageProvider(RedisClient redisClient) {
        this(redisClient, rateLimit().at1Request().per(SECOND));
    }

    public LettuceRedisStorageProvider(RedisClient redisClient, RateLimiter changeListenerNotificationRateLimit) {
        this(redisClient, null, changeListenerNotificationRateLimit);
    }

    public LettuceRedisStorageProvider(RedisClient redisClient, String keyPrefix) {
        this(redisClient, keyPrefix, rateLimit().at1Request().per(SECOND));
    }

    public LettuceRedisStorageProvider(RedisClient redisClient, String keyPrefix, RateLimiter changeListenerNotificationRateLimit) {
        this(ConnectionPoolSupport.createGenericObjectPool(redisClient::connect, new GenericObjectPoolConfig<>()), keyPrefix, changeListenerNotificationRateLimit);
    }

    public LettuceRedisStorageProvider(ObjectPool<StatefulRedisConnection<String, String>> pool) {
        this(pool, null, rateLimit().at1Request().per(SECOND));
    }

    public LettuceRedisStorageProvider(ObjectPool<StatefulRedisConnection<String, String>> pool, String keyPrefix) {
        this(pool, keyPrefix, rateLimit().at1Request().per(SECOND));
    }

    public LettuceRedisStorageProvider(ObjectPool<StatefulRedisConnection<String, String>> pool, String keyPrefix, RateLimiter changeListenerNotificationRateLimit) {
        super(changeListenerNotificationRateLimit);
        this.pool = pool;
        this.keyPrefix = isNullOrEmpty(keyPrefix) ? "" : keyPrefix;

        setUpStorageProvider(DatabaseOptions.CREATE);
    }

    @Override
    public void setJobMapper(JobMapper jobMapper) {
        this.jobMapper = jobMapper;
    }

    @Override
    public void setUpStorageProvider(DatabaseOptions databaseOptions) {
        if(DatabaseOptions.CREATE != databaseOptions) throw new IllegalArgumentException("LattuceRedisStorageProvider only supports CREATE as databaseOptions.");
        new LettuceRedisDBCreator(this, pool, keyPrefix).runMigrations();
    }

    @Override
    public void announceBackgroundJobServer(BackgroundJobServerStatus serverStatus) {
        try (final StatefulRedisConnection<String, String> connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            commands.multi();
            commands.hset(backgroundJobServerKey(keyPrefix, serverStatus), BackgroundJobServers.FIELD_ID, serverStatus.getId().toString());
            commands.hset(backgroundJobServerKey(keyPrefix, serverStatus), BackgroundJobServers.FIELD_WORKER_POOL_SIZE, String.valueOf(serverStatus.getWorkerPoolSize()));
            commands.hset(backgroundJobServerKey(keyPrefix, serverStatus), BackgroundJobServers.FIELD_POLL_INTERVAL_IN_SECONDS, String.valueOf(serverStatus.getPollIntervalInSeconds()));
            commands.hset(backgroundJobServerKey(keyPrefix, serverStatus), BackgroundJobServers.FIELD_DELETE_SUCCEEDED_JOBS_AFTER, String.valueOf(serverStatus.getDeleteSucceededJobsAfter()));
            commands.hset(backgroundJobServerKey(keyPrefix, serverStatus), BackgroundJobServers.FIELD_DELETE_DELETED_JOBS_AFTER, String.valueOf(serverStatus.getPermanentlyDeleteDeletedJobsAfter()));
            commands.hset(backgroundJobServerKey(keyPrefix, serverStatus), BackgroundJobServers.FIELD_FIRST_HEARTBEAT, String.valueOf(serverStatus.getFirstHeartbeat()));
            commands.hset(backgroundJobServerKey(keyPrefix, serverStatus), BackgroundJobServers.FIELD_LAST_HEARTBEAT, String.valueOf(serverStatus.getLastHeartbeat()));
            commands.hset(backgroundJobServerKey(keyPrefix, serverStatus), BackgroundJobServers.FIELD_IS_RUNNING, String.valueOf(serverStatus.isRunning()));
            commands.hset(backgroundJobServerKey(keyPrefix, serverStatus), BackgroundJobServers.FIELD_SYSTEM_TOTAL_MEMORY, String.valueOf(serverStatus.getSystemTotalMemory()));
            commands.hset(backgroundJobServerKey(keyPrefix, serverStatus), BackgroundJobServers.FIELD_SYSTEM_FREE_MEMORY, String.valueOf(serverStatus.getSystemFreeMemory()));
            commands.hset(backgroundJobServerKey(keyPrefix, serverStatus), BackgroundJobServers.FIELD_SYSTEM_CPU_LOAD, String.valueOf(serverStatus.getSystemCpuLoad()));
            commands.hset(backgroundJobServerKey(keyPrefix, serverStatus), BackgroundJobServers.FIELD_PROCESS_MAX_MEMORY, String.valueOf(serverStatus.getProcessMaxMemory()));
            commands.hset(backgroundJobServerKey(keyPrefix, serverStatus), BackgroundJobServers.FIELD_PROCESS_FREE_MEMORY, String.valueOf(serverStatus.getProcessFreeMemory()));
            commands.hset(backgroundJobServerKey(keyPrefix, serverStatus), BackgroundJobServers.FIELD_PROCESS_ALLOCATED_MEMORY, String.valueOf(serverStatus.getProcessAllocatedMemory()));
            commands.hset(backgroundJobServerKey(keyPrefix, serverStatus), BackgroundJobServers.FIELD_PROCESS_CPU_LOAD, String.valueOf(serverStatus.getProcessCpuLoad()));
            commands.zadd(backgroundJobServersCreatedKey(keyPrefix), toMicroSeconds(now()), serverStatus.getId().toString());
            commands.zadd(backgroundJobServersUpdatedKey(keyPrefix), toMicroSeconds(now()), serverStatus.getId().toString());
            commands.exec();
        }
    }

    @Override
    public boolean signalBackgroundJobServerAlive(BackgroundJobServerStatus serverStatus) {
        try (final StatefulRedisConnection<String, String> connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            final Map<String, String> valueMap = commands.hgetall(backgroundJobServerKey(keyPrefix, serverStatus));
            if (valueMap.isEmpty())
                throw new ServerTimedOutException(serverStatus, new StorageException("BackgroundJobServer with id " + serverStatus.getId() + " was not found"));

            commands.watch(backgroundJobServerKey(keyPrefix, serverStatus));
            commands.multi();
            commands.hset(backgroundJobServerKey(keyPrefix, serverStatus), BackgroundJobServers.FIELD_LAST_HEARTBEAT, String.valueOf(serverStatus.getLastHeartbeat()));
            commands.hset(backgroundJobServerKey(keyPrefix, serverStatus), BackgroundJobServers.FIELD_SYSTEM_FREE_MEMORY, String.valueOf(serverStatus.getSystemFreeMemory()));
            commands.hset(backgroundJobServerKey(keyPrefix, serverStatus), BackgroundJobServers.FIELD_SYSTEM_CPU_LOAD, String.valueOf(serverStatus.getSystemCpuLoad()));
            commands.hset(backgroundJobServerKey(keyPrefix, serverStatus), BackgroundJobServers.FIELD_PROCESS_FREE_MEMORY, String.valueOf(serverStatus.getProcessFreeMemory()));
            commands.hset(backgroundJobServerKey(keyPrefix, serverStatus), BackgroundJobServers.FIELD_PROCESS_ALLOCATED_MEMORY, String.valueOf(serverStatus.getProcessAllocatedMemory()));
            commands.hset(backgroundJobServerKey(keyPrefix, serverStatus), BackgroundJobServers.FIELD_PROCESS_CPU_LOAD, String.valueOf(serverStatus.getProcessCpuLoad()));
            commands.zadd(backgroundJobServersUpdatedKey(keyPrefix), toMicroSeconds(now()), serverStatus.getId().toString());
            commands.exec();
            return Boolean.parseBoolean(commands.hget(backgroundJobServerKey(keyPrefix, serverStatus), BackgroundJobServers.FIELD_IS_RUNNING));
        }
    }

    @Override
    public void signalBackgroundJobServerStopped(BackgroundJobServerStatus serverStatus) {
        try (final StatefulRedisConnection<String, String> connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            commands.multi();
            commands.del(backgroundJobServerKey(keyPrefix, serverStatus.getId()));
            commands.zrem(backgroundJobServersCreatedKey(keyPrefix), serverStatus.getId().toString());
            commands.zrem(backgroundJobServersUpdatedKey(keyPrefix), serverStatus.getId().toString());
            commands.exec();
        }
    }

    @Override
    public List<BackgroundJobServerStatus> getBackgroundJobServers() {
        try (final StatefulRedisConnection<String, String> connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            List<String> zrange = commands.zrange(backgroundJobServersCreatedKey(keyPrefix), 0, Integer.MAX_VALUE);
            return new LettuceRedisPipelinedStream<>(zrange, connection)
                    .mapUsingPipeline((p, id) -> p.hgetall(backgroundJobServerKey(keyPrefix, id)))
                    .mapAfterSync(RedisFuture<Map<String, String>>::get)
                    .map(fieldMap -> new BackgroundJobServerStatus(
                            UUID.fromString(fieldMap.get(BackgroundJobServers.FIELD_ID)),
                            Integer.parseInt(fieldMap.get(BackgroundJobServers.FIELD_WORKER_POOL_SIZE)),
                            Integer.parseInt(fieldMap.get(BackgroundJobServers.FIELD_POLL_INTERVAL_IN_SECONDS)),
                            Duration.parse(fieldMap.get(BackgroundJobServers.FIELD_DELETE_SUCCEEDED_JOBS_AFTER)),
                            Duration.parse(fieldMap.get(BackgroundJobServers.FIELD_DELETE_DELETED_JOBS_AFTER)),
                            Instant.parse(fieldMap.get(BackgroundJobServers.FIELD_FIRST_HEARTBEAT)),
                            Instant.parse(fieldMap.get(BackgroundJobServers.FIELD_LAST_HEARTBEAT)),
                            Boolean.parseBoolean(fieldMap.get(BackgroundJobServers.FIELD_IS_RUNNING)),
                            parseLong(fieldMap.get(BackgroundJobServers.FIELD_SYSTEM_TOTAL_MEMORY)),
                            parseLong(fieldMap.get(BackgroundJobServers.FIELD_SYSTEM_FREE_MEMORY)),
                            Double.parseDouble(fieldMap.get(BackgroundJobServers.FIELD_SYSTEM_CPU_LOAD)),
                            parseLong(fieldMap.get(BackgroundJobServers.FIELD_PROCESS_MAX_MEMORY)),
                            parseLong(fieldMap.get(BackgroundJobServers.FIELD_PROCESS_FREE_MEMORY)),
                            parseLong(fieldMap.get(BackgroundJobServers.FIELD_PROCESS_ALLOCATED_MEMORY)),
                            Double.parseDouble(fieldMap.get(BackgroundJobServers.FIELD_PROCESS_CPU_LOAD))
                    ))
                    .collect(toList());
        }
    }

    @Override
    public UUID getLongestRunningBackgroundJobServerId() {
        try (final StatefulRedisConnection<String, String> connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            return (commands.zrange(backgroundJobServersCreatedKey(keyPrefix), 0, 1)).stream()
                    .map(UUID::fromString)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No servers available?!"));
        }
    }

    @Override
    public int removeTimedOutBackgroundJobServers(Instant heartbeatOlderThan) {
        try (final StatefulRedisConnection<String, String> connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            final List<String> backgroundJobServers = commands.zrangebyscore(backgroundJobServersUpdatedKey(keyPrefix), Range.create(0, toMicroSeconds(heartbeatOlderThan)));
            commands.multi();
            backgroundJobServers.forEach(backgroundJobServerId -> {
                commands.del(backgroundJobServerKey(keyPrefix, backgroundJobServerId));
                commands.zrem(backgroundJobServersCreatedKey(keyPrefix), backgroundJobServerId);
                commands.zrem(backgroundJobServersUpdatedKey(keyPrefix), backgroundJobServerId);
            });
            commands.exec();
            return backgroundJobServers.size();
        }
    }

    @Override
    public void saveMetadata(JobRunrMetadata metadata) {
        try (final StatefulRedisConnection<String, String> connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            commands.multi();
            commands.hset(metadataKey(keyPrefix, metadata), Metadata.FIELD_ID, metadata.getId());
            commands.hset(metadataKey(keyPrefix, metadata), Metadata.FIELD_NAME, metadata.getName());
            commands.hset(metadataKey(keyPrefix, metadata), Metadata.FIELD_OWNER, metadata.getOwner());
            commands.hset(metadataKey(keyPrefix, metadata), Metadata.FIELD_VALUE, metadata.getValue());
            commands.hset(metadataKey(keyPrefix, metadata), Metadata.FIELD_CREATED_AT, String.valueOf(metadata.getCreatedAt()));
            commands.hset(metadataKey(keyPrefix, metadata), Metadata.FIELD_UPDATED_AT, String.valueOf(metadata.getUpdatedAt()));
            commands.sadd(metadatasKey(keyPrefix), metadataKey(keyPrefix, metadata));
            commands.exec();
            notifyMetadataChangeListeners();
        }
    }

    @Override
    public List<JobRunrMetadata> getMetadata(String name) {
        try (final StatefulRedisConnection<String, String> connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();

            return (commands.smembers(metadatasKey(keyPrefix))).stream()
                    .filter(metadataName -> metadataName.startsWith(metadataKey(keyPrefix, name + "-")))
                    .map(commands::hgetall)
                    .map(fieldMap -> new JobRunrMetadata(
                            fieldMap.get(Metadata.FIELD_NAME),
                            fieldMap.get(Metadata.FIELD_OWNER),
                            fieldMap.get(Metadata.FIELD_VALUE),
                            Instant.parse(fieldMap.get(Metadata.FIELD_CREATED_AT)),
                            Instant.parse(fieldMap.get(Metadata.FIELD_UPDATED_AT))
                    ))
                    .collect(Collectors.toList());
        }
    }

    @Override
    public JobRunrMetadata getMetadata(String name, String owner) {
        try (final StatefulRedisConnection<String, String> connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            Map<String, String> fieldMap = commands.hgetall(metadataKey(keyPrefix, toId(name, owner)));
            return new JobRunrMetadata(
                    fieldMap.get(Metadata.FIELD_NAME),
                    fieldMap.get(Metadata.FIELD_OWNER),
                    fieldMap.get(Metadata.FIELD_VALUE),
                    Instant.parse(fieldMap.get(Metadata.FIELD_CREATED_AT)),
                    Instant.parse(fieldMap.get(Metadata.FIELD_UPDATED_AT))
            );
        }
    }

    @Override
    public void deleteMetadata(String name) {
        try (final StatefulRedisConnection<String, String> connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            List<String> metadataToDelete = (commands.smembers(metadatasKey(keyPrefix))).stream()
                    .filter(metadataName -> metadataName.startsWith(metadataKey(keyPrefix, name + "-")))
                    .collect(toList());

            if (!metadataToDelete.isEmpty()) {
                commands.multi();
                metadataToDelete.forEach(metadataName -> {
                    commands.del(metadataName);
                    commands.srem(metadatasKey(keyPrefix), metadataName);
                });
                commands.exec();
                notifyMetadataChangeListeners();
            }
        }
    }

    @Override
    public Job save(Job jobToSave) {
        try (final StatefulRedisConnection<String, String> connection = getConnection(); JobVersioner jobVersioner = new JobVersioner(jobToSave)) {
            RedisCommands<String, String> commands = connection.sync();
            if (jobVersioner.isNewJob()) {
                insertJob(jobToSave, commands);
            } else {
                updateJob(jobToSave, commands);
            }
            jobVersioner.commitVersion();
            notifyJobStatsOnChangeListeners();
            return jobToSave;
        } catch (RedisException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public int deletePermanently(UUID id) {
        Job job = getJobById(id);
        try (final StatefulRedisConnection<String, String> connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            commands.multi();
            commands.del(jobKey(keyPrefix, job));
            commands.del(jobVersionKey(keyPrefix, job));
            deleteJobMetadata(commands, job);
            final TransactionResult result = commands.exec();
            int amount = result == null || result.isEmpty() ? 0 : 1;
            notifyJobStatsOnChangeListenersIf(amount > 0);
            return amount;
        }
    }

    @Override
    public Job getJobById(UUID id) {
        try (final StatefulRedisConnection<String, String> connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            final Object serializedJob = commands.get(jobKey(keyPrefix, id));
            if (serializedJob == null) throw new JobNotFoundException(id);
            return jobMapper.deserializeJob(serializedJob.toString());
        }
    }

    @Override
    public List<Job> save(List<Job> jobs) {
        if (jobs.isEmpty()) return jobs;

        try (final StatefulRedisConnection<String, String> connection = getConnection(); final JobListVersioner jobListVersioner = new JobListVersioner(jobs)) {
            RedisCommands<String, String> commands = connection.sync();
            if (jobListVersioner.areNewJobs()) {
                commands.multi();
                jobs.forEach(jobToSave -> saveJob(commands, jobToSave));
                commands.exec();
            } else {
                final List<Job> concurrentModifiedJobs = returnConcurrentModifiedJobs(jobs, job -> updateJob(job, commands));
                if (!concurrentModifiedJobs.isEmpty()) {
                    jobListVersioner.rollbackVersions(concurrentModifiedJobs);
                    throw new ConcurrentJobModificationException(concurrentModifiedJobs);
                }
            }
            jobListVersioner.commitVersions();
            notifyJobStatsOnChangeListenersIf(!jobs.isEmpty());
            return jobs;
        } catch (RedisException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public List<Job> getJobs(StateName state, Instant updatedBefore, PageRequest pageRequest) {
        try (final StatefulRedisConnection<String, String> connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            List<String> jobsByState;
            if ("updatedAt:ASC".equals(pageRequest.getOrder())) {
                jobsByState = commands.zrangebyscore(jobQueueForStateKey(keyPrefix, state), Range.create(0, toMicroSeconds(updatedBefore)), Limit.create(pageRequest.getOffset(), pageRequest.getLimit()));
            } else if ("updatedAt:DESC".equals(pageRequest.getOrder())) {
                jobsByState = commands.zrevrangebyscore(jobQueueForStateKey(keyPrefix, state), Range.create(0, toMicroSeconds(updatedBefore)), Limit.create(pageRequest.getOffset(), pageRequest.getLimit()));
            } else {
                throw new IllegalArgumentException("Unsupported sorting: " + pageRequest.getOrder());
            }
            return new LettuceRedisPipelinedStream<>(jobsByState, connection)
                    .mapUsingPipeline((p, id) -> p.get(jobKey(keyPrefix, id)))
                    .mapAfterSync(RedisFuture<String>::get)
                    .map(jobMapper::deserializeJob)
                    .collect(toList());
        }
    }

    @Override
    public List<Job> getScheduledJobs(Instant scheduledBefore, PageRequest pageRequest) {
        try (final StatefulRedisConnection<String, String> connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            return new LettuceRedisPipelinedStream<>(commands.zrangebyscore(scheduledJobsKey(keyPrefix), Range.create(0, toMicroSeconds(now())), Limit.create(pageRequest.getOffset(), pageRequest.getLimit())), connection)
                    .mapUsingPipeline((p, id) -> p.get(jobKey(keyPrefix, id)))
                    .mapAfterSync(RedisFuture<String>::get)
                    .map(jobMapper::deserializeJob)
                    .collect(toList());
        }
    }

    @Override
    public List<Job> getJobs(StateName state, PageRequest pageRequest) {
        try (final StatefulRedisConnection<String, String> connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            List<String> jobsByState;
            // we only support what is used by frontend
            if ("updatedAt:ASC".equals(pageRequest.getOrder())) {
                jobsByState = commands.zrange(jobQueueForStateKey(keyPrefix, state), pageRequest.getOffset(), pageRequest.getOffset() + pageRequest.getLimit() - 1);
            } else if ("updatedAt:DESC".equals(pageRequest.getOrder())) {
                jobsByState = commands.zrevrange(jobQueueForStateKey(keyPrefix, state), pageRequest.getOffset(), pageRequest.getOffset() + pageRequest.getLimit() - 1);
            } else {
                throw new IllegalArgumentException("Unsupported sorting: " + pageRequest.getOrder());
            }
            return new LettuceRedisPipelinedStream<>(jobsByState, connection)
                    .mapUsingPipeline((p, id) -> p.get(jobKey(keyPrefix, id)))
                    .mapAfterSync(RedisFuture<String>::get)
                    .map(jobMapper::deserializeJob)
                    .collect(toList());
        }
    }

    @Override
    public Page<Job> getJobPage(StateName state, PageRequest pageRequest) {
        try (final StatefulRedisConnection<String, String> connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            long count = commands.zcount(jobQueueForStateKey(keyPrefix, state), unbounded());
            if (count > 0) {
                List<Job> jobs = getJobs(state, pageRequest);
                return new Page<>(count, jobs, pageRequest);
            }
            return new Page<>(0, new ArrayList<>(), pageRequest);
        }
    }

    @Override
    public int deleteJobsPermanently(StateName state, Instant updatedBefore) {
        int amount = 0;
        try (final StatefulRedisConnection<String, String> connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            List<String> zrangeToInspect = commands.zrange(jobQueueForStateKey(keyPrefix, state), 0, 1000);
            outerloop:
            while (!zrangeToInspect.isEmpty()) {
                for (String id : zrangeToInspect) {
                    final Job job = getJobById(UUID.fromString(id));
                    if (job.getUpdatedAt().isAfter(updatedBefore)) break outerloop;

                    commands.multi();
                    commands.del(jobKey(keyPrefix, job));
                    commands.del(jobVersionKey(keyPrefix, job));
                    deleteJobMetadata(commands, job);

                    final TransactionResult exec = commands.exec();
                    if (exec != null && !exec.isEmpty()) amount++;
                }
                zrangeToInspect = commands.zrange(jobQueueForStateKey(keyPrefix, state), 0, 1000);
            }
        }

        notifyJobStatsOnChangeListenersIf(amount > 0);
        return amount;
    }

    @Override
    public Set<String> getDistinctJobSignatures(StateName... states) {
        try (final StatefulRedisConnection<String, String> connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            List<Set<String>> jobSignatures = stream(states)
                    .map(stateName -> commands.smembers(jobDetailsKey(keyPrefix, stateName)))
                    .collect(toList());
            return jobSignatures.stream().flatMap(Collection::stream).collect(toSet());
        }
    }

    @Override
    public boolean exists(JobDetails jobDetails, StateName... states) {
        try (final StatefulRedisConnection<String, String> connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            List<Boolean> existsJob = stream(states)
                    .map(stateName -> commands.sismember(jobDetailsKey(keyPrefix, stateName), getJobSignature(jobDetails)))
                    .collect(toList());
            return existsJob.stream().filter(b -> b).findAny().orElse(false);
        }
    }

    @Override
    public boolean recurringJobExists(String recurringJobId, StateName... states) {
        try (final StatefulRedisConnection<String, String> connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            List<Boolean> existsJob = stream(states)
                    .map(stateName -> commands.sismember(recurringJobKey(keyPrefix, stateName), recurringJobId))
                    .collect(toList());
            return existsJob.stream().filter(b -> b).findAny().orElse(false);
        }
    }

    @Override
    public RecurringJob saveRecurringJob(RecurringJob recurringJob) {
        try (final StatefulRedisConnection<String, String> connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            commands.multi();
            commands.set(recurringJobKey(keyPrefix, recurringJob.getId()), jobMapper.serializeRecurringJob(recurringJob));
            commands.sadd(recurringJobsKey(keyPrefix), recurringJob.getId());
            commands.exec();
        }
        return recurringJob;
    }

    @Override
    public List<RecurringJob> getRecurringJobs() {
        try (final StatefulRedisConnection<String, String> connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            return commands.smembers(recurringJobsKey(keyPrefix))
                    .stream()
                    .map(id -> commands.get(recurringJobKey(keyPrefix, id)))
                    .map(jobMapper::deserializeRecurringJob)
                    .collect(toList());
        }
    }

    @Override
    public long countRecurringJobs() {
        try (final StatefulRedisConnection<String, String> connection = getConnection()) {
            return connection.sync().scard(recurringJobsKey(keyPrefix));
        }
    }

    @Override
    public int deleteRecurringJob(String id) {
        try (final StatefulRedisConnection<String, String> connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            commands.multi();
            commands.del(recurringJobKey(keyPrefix, id));
            commands.srem(recurringJobsKey(keyPrefix), id);

            final TransactionResult exec = commands.exec();
            return (exec != null && !exec.isEmpty()) ? 1 : 0;
        }
    }

    @Override
    public JobStats getJobStats() {
        Instant instant = Instant.now();
        try (final StatefulRedisConnection<String, String> connection = getConnection()) {
            connection.setAutoFlushCommands(false);
            RedisAsyncCommands<String, String> commands = connection.async();
            final RedisFuture<String> totalSucceededAmountCounterResponse = commands.hget(metadataKey(keyPrefix, Metadata.STATS_ID), Metadata.FIELD_VALUE);

            final RedisFuture<Long> scheduledResponse = commands.zcount(jobQueueForStateKey(keyPrefix, SCHEDULED), unbounded());
            final RedisFuture<Long> enqueuedResponse = commands.zcount(jobQueueForStateKey(keyPrefix, ENQUEUED), unbounded());
            final RedisFuture<Long> processingResponse = commands.zcount(jobQueueForStateKey(keyPrefix, PROCESSING), unbounded());
            final RedisFuture<Long> succeededResponse = commands.zcount(jobQueueForStateKey(keyPrefix, SUCCEEDED), unbounded());
            final RedisFuture<Long> failedResponse = commands.zcount(jobQueueForStateKey(keyPrefix, FAILED), unbounded());
            final RedisFuture<Long> deletedResponse = commands.zcount(jobQueueForStateKey(keyPrefix, DELETED), unbounded());

            final RedisFuture<Long> recurringJobsResponse = commands.scard(recurringJobsKey(keyPrefix));
            final RedisFuture<Long> backgroundJobServerResponse = commands.zcount(backgroundJobServersUpdatedKey(keyPrefix), unbounded());

            connection.flushCommands();
            LettuceFutures.awaitAll(Duration.ofSeconds(10), totalSucceededAmountCounterResponse, scheduledResponse,
                    enqueuedResponse, processingResponse, succeededResponse, failedResponse, deletedResponse);

            final long scheduledCount = getCounterValue(scheduledResponse);
            final long enqueuedCount = getCounterValue(enqueuedResponse);
            final long processingCount = getCounterValue(processingResponse);
            final long succeededCount = getCounterValue(succeededResponse);
            final long allTimeSucceededCount = getAllTimeSucceededCounterValue(totalSucceededAmountCounterResponse);
            final long failedCount = getCounterValue(failedResponse);
            final long deletedCount = getCounterValue(deletedResponse);
            final long total = scheduledCount + enqueuedCount + processingCount + succeededCount + failedCount;
            final long recurringJobsCount = getCounterValue(recurringJobsResponse);
            final long backgroundJobServerCount = getCounterValue(backgroundJobServerResponse);
            return new JobStats(
                    instant,
                    total,
                    scheduledCount,
                    enqueuedCount,
                    processingCount,
                    failedCount,
                    succeededCount,
                    allTimeSucceededCount,
                    deletedCount,
                    (int) recurringJobsCount,
                    (int) backgroundJobServerCount
            );
        }
    }

    @Override
    public void publishTotalAmountOfSucceededJobs(int amount) {
        try (final StatefulRedisConnection<String, String> connection = getConnection()) {
            RedisCommands<String, String> commands = connection.sync();
            commands.hincrby(metadataKey(keyPrefix, Metadata.STATS_ID), Metadata.FIELD_VALUE, amount);
        }
    }

    @Override
    public void close() {
        super.close();
        pool.close();
    }

    private void insertJob(Job jobToSave, RedisCommands<String, String> commands) {
        if (commands.exists(jobKey(keyPrefix, jobToSave)) > 0) throw new ConcurrentJobModificationException(jobToSave);
        commands.multi();
        saveJob(commands, jobToSave);
        TransactionResult result = commands.exec();
        if (result.wasDiscarded()) {
            throw new StorageException("Unable to save job " + jobToSave.getId() + " with version " + jobToSave.getVersion());
        }
    }

    private void updateJob(Job jobToSave, RedisCommands<String, String> commands) {
        commands.watch(jobVersionKey(keyPrefix, jobToSave));
        final int version = Integer.parseInt(commands.get(jobVersionKey(keyPrefix, jobToSave)));
        if (version != (jobToSave.getVersion() - 1)) throw new ConcurrentJobModificationException(jobToSave);
        commands.multi();
        saveJob(commands, jobToSave);
        TransactionResult result = commands.exec();
        if (result == null || result.isEmpty()) {
            throw new ConcurrentJobModificationException(jobToSave);
        }
    }

    private void saveJob(RedisCommands<String, String> commands, Job jobToSave) {
        deleteJobMetadataForUpdate(commands, jobToSave);
        commands.set(jobVersionKey(keyPrefix, jobToSave), String.valueOf(jobToSave.getVersion()));
        commands.set(jobKey(keyPrefix, jobToSave), jobMapper.serializeJob(jobToSave));
        commands.zadd(jobQueueForStateKey(keyPrefix, jobToSave.getState()), toMicroSeconds(jobToSave.getUpdatedAt()), jobToSave.getId().toString());
        commands.sadd(jobDetailsKey(keyPrefix, jobToSave.getState()), getJobSignature(jobToSave.getJobDetails()));
        if (SCHEDULED.equals(jobToSave.getState())) {
            commands.zadd(scheduledJobsKey(keyPrefix), toMicroSeconds(((ScheduledState) jobToSave.getJobState()).getScheduledAt()), jobToSave.getId().toString());
        }
        jobToSave.getRecurringJobId().ifPresent(recurringJobId -> commands.sadd(recurringJobKey(keyPrefix, jobToSave.getState()), recurringJobId));
    }

    private void deleteJobMetadataForUpdate(RedisCommands<String, String> commands, Job job) {
        String id = job.getId().toString();
        commands.zrem(scheduledJobsKey(keyPrefix), id);
        Stream.of(StateName.values()).forEach(stateName -> commands.zrem(jobQueueForStateKey(keyPrefix, stateName), id));
        Stream.of(StateName.values()).filter(stateName -> !SCHEDULED.equals(stateName)).forEach(stateName -> commands.srem(jobDetailsKey(keyPrefix, stateName), getJobSignature(job.getJobDetails())));
        if ((job.hasState(ENQUEUED) && job.getJobStates().size() >= 2 && job.getJobState(-2) instanceof ScheduledState)
                || (job.hasState(DELETED) && job.getJobStates().size() >= 2 && job.getJobState(-2) instanceof ScheduledState)) {
            commands.srem(jobDetailsKey(keyPrefix, SCHEDULED), getJobSignature(job.getJobDetails()));
        }
        job.getRecurringJobId().ifPresent(recurringJobId -> Stream.of(StateName.values()).forEach(stateName -> commands.srem(recurringJobKey(keyPrefix, stateName), recurringJobId)));
    }

    private void deleteJobMetadata(RedisCommands<String, String> commands, Job job) {
        String id = job.getId().toString();
        commands.zrem(scheduledJobsKey(keyPrefix), id);
        Stream.of(StateName.values()).forEach(stateName -> commands.zrem(jobQueueForStateKey(keyPrefix, stateName), id));
        Stream.of(StateName.values()).forEach(stateName -> commands.srem(jobDetailsKey(keyPrefix, stateName), getJobSignature(job.getJobDetails())));
    }

    private long getCounterValue(RedisFuture<Long> countResponse) {
        try {
            return countResponse.get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            return 0L;
        }
    }

    private long getAllTimeSucceededCounterValue(RedisFuture<String> allTimeSucceededAmountCounterResponse) {
        try {
            return parseLong(allTimeSucceededAmountCounterResponse.get());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            return 0L;
        }
    }

    protected StatefulRedisConnection<String, String> getConnection() {
        try {
            StatefulRedisConnection<String, String> statefulRedisConnection = pool.borrowObject();
            statefulRedisConnection.setAutoFlushCommands(true);
            return statefulRedisConnection;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
