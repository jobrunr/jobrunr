package org.jobrunr.storage.nosql.redis;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.jobrunr.storage.JobNotFoundException;
import org.jobrunr.storage.JobStats;
import org.jobrunr.storage.JobStorageChangeListener;
import org.jobrunr.storage.Page;
import org.jobrunr.storage.PageRequest;
import org.jobrunr.storage.ServerTimedOutException;
import org.jobrunr.storage.StorageException;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.resilience.RateLimiter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.commands.RedisPipeline;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
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

public class RedisStorageProvider implements StorageProvider {

    private final Set<JobStorageChangeListener> onChangeListeners = new HashSet<>();
    private final RateLimiter changeListenerNotificationRateLimit;

    private Jedis jedis;
    private JobMapper jobMapper;

    public RedisStorageProvider() {
        this(new Jedis());
    }

    public RedisStorageProvider(Jedis jedis) {
        this(jedis, rateLimit().at2Requests().per(SECOND));
    }

    public RedisStorageProvider(Jedis jedis, RateLimiter changeListenerNotificationRateLimit) {
        this.jedis = jedis;
        this.changeListenerNotificationRateLimit = changeListenerNotificationRateLimit;
    }

    @Override
    public void addJobStorageOnChangeListener(JobStorageChangeListener listener) {
        onChangeListeners.add(listener);
    }

    @Override
    public void setJobMapper(JobMapper jobMapper) {
        this.jobMapper = jobMapper;
    }

    @Override
    public void announceBackgroundJobServer(BackgroundJobServerStatus serverStatus) {
        try (Jedis jedis = getJedis()) {
            final Pipeline p = jedis.pipelined();
            p.hset("backgroundjobserver:" + serverStatus.getId(), "id", serverStatus.getId().toString());
            p.hset("backgroundjobserver:" + serverStatus.getId(), "workerPoolSize", String.valueOf(serverStatus.getWorkerPoolSize()));
            p.hset("backgroundjobserver:" + serverStatus.getId(), "pollIntervalInSeconds", String.valueOf(serverStatus.getPollIntervalInSeconds()));
            p.hset("backgroundjobserver:" + serverStatus.getId(), "firstHeartbeat", String.valueOf(serverStatus.getFirstHeartbeat()));
            p.hset("backgroundjobserver:" + serverStatus.getId(), "lastHeartbeat", String.valueOf(Instant.now()));
            p.hset("backgroundjobserver:" + serverStatus.getId(), "isRunning", String.valueOf(serverStatus.isRunning()));
            p.zadd("backgroundjobservers", toMicroSeconds(now()), serverStatus.getId().toString());
            p.sync();
        }
    }

    @Override
    public boolean signalBackgroundJobServerAlive(BackgroundJobServerStatus serverStatus) {
        try (Jedis jedis = getJedis()) {
            final Map<String, String> valueMap = jedis.hgetAll("backgroundjobserver:" + serverStatus.getId());
            if (valueMap.isEmpty()) throw new ServerTimedOutException(serverStatus, new StorageException("BackgroundJobServer with id " + serverStatus.getId() + " was not found"));
            final Pipeline p = jedis.pipelined();
            p.watch("backgroundjobserver:" + serverStatus.getId());
            p.hset("backgroundjobserver:" + serverStatus.getId(), "lastHeartbeat", String.valueOf(Instant.now()));
            final Response<String> isRunningResponse = p.hget("backgroundjobserver:" + serverStatus.getId(), "isRunning");
            p.sync();
            return Boolean.parseBoolean(isRunningResponse.get());
        }
    }

    @Override
    public List<BackgroundJobServerStatus> getBackgroundJobServers() {
        try (Jedis jedis = getJedis()) {
            return new RedisPipelinedStream<>(jedis.zrange("backgroundjobservers", 0, Integer.MAX_VALUE), jedis)
                    .mapUsingPipeline((p, id) -> p.hgetAll("backgroundjobserver:" + id))
                    .mapAfterSync(Response::get)
                    .map(fieldMap -> new BackgroundJobServerStatus(
                            UUID.fromString(fieldMap.get("id")),
                            Integer.parseInt(fieldMap.get("workerPoolSize")),
                            Integer.parseInt(fieldMap.get("pollIntervalInSeconds")),
                            Instant.parse(fieldMap.get("firstHeartbeat")),
                            Instant.parse(fieldMap.get("lastHeartbeat")),
                            Boolean.parseBoolean(fieldMap.get("isRunning"))
                    ))
                    .collect(Collectors.toList());
        }
    }

    @Override
    public int removeTimedOutBackgroundJobServers(Instant heartbeatOlderThan) {
        try (Jedis jedis = getJedis()) {
            final Set<String> backgroundjobservers = jedis.zrangeByScore("backgroundjobservers", 0, toMicroSeconds(heartbeatOlderThan));
            final Pipeline p = jedis.pipelined();

            backgroundjobservers.forEach(backgroundJobServerId -> {
                p.del("backgroundjobserver:" + backgroundJobServerId);
                p.zrem("backgroundjobservers", backgroundJobServerId);
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
                jobToSave.setId(UUID.randomUUID());
                Pipeline p = jedis.pipelined();
                saveJob(p, jobToSave);
                p.publish("job-queue-channel", jobToSave.getId().toString());
                p.sync();
            } else {
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
            jobs.forEach(this::save);
        }
        notifyOnChangeListenersIf(jobs.size() > 0);
        return jobs;
    }

    @Override
    public List<Job> getJobs(StateName state, Instant updatedBefore, PageRequest pageRequest) {
        try (Jedis jedis = getJedis()) {
            return new RedisPipelinedStream<>(jedis.zrangeByScore("queue:jobs:" + state, 0, toMicroSeconds(updatedBefore)), jedis)
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
        return new RedisPipelinedStream<>(jedis.zrangeByScore("queue:scheduledjobs", 0, toMicroSeconds(now())), jedis)
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
            return jedis.zcount("queue:jobs:" + state, 0, Long.MAX_VALUE);
        }
    }

    @Override
    public List<Job> getJobs(StateName state, PageRequest pageRequest) {
        try (Jedis jedis = getJedis()) {
            return new RedisPipelinedStream<>(jedis.zrange("queue:jobs:" + state, pageRequest.getOffset(), pageRequest.getOffset() + pageRequest.getLimit() - 1), jedis)
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
            Set<String> zrangeToInspect = jedis.zrange("queue:jobs:" + state, 0, 1000);
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
                zrangeToInspect = jedis.zrange("queue:jobs:" + state, 0, 1000);
            }
        }
        notifyOnChangeListenersIf(amount > 0);
        return amount;
    }

    @Override
    public boolean exists(JobDetails jobDetails, StateName state) {
        try (Jedis jedis = getJedis()) {
            return jedis.sismember("job:jobdetails:" + state, getJobSignature(jobDetails));
        }
    }

    @Override
    public RecurringJob saveRecurringJob(RecurringJob recurringJob) {
        try (Jedis jedis = getJedis()) {
            final Pipeline p = jedis.pipelined();
            p.set("recurringjob:" + recurringJob.getId(), jobMapper.serializeRecurringJob(recurringJob));
            p.sadd("recurringjobs", recurringJob.getId());
            p.sync();
        }
        return recurringJob;
    }

    @Override
    public List<RecurringJob> getRecurringJobs() {
        try (Jedis jedis = getJedis()) {
            return jedis.smembers("recurringjobs")
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
            transaction.srem("recurringjobs", id);

            final List<Object> exec = transaction.exec();
            return (exec != null && exec.size() > 0) ? 1 : 0;
        }
    }

    @Override
    public JobStats getJobStats() {
        try (Jedis jedis = getJedis()) {
            final Pipeline p = jedis.pipelined();
            final Response<String> waitingCounterResponse = p.get("counter:jobs:" + AWAITING);
            final Response<Long> waitingResponse = p.zcount("queue:jobs:" + AWAITING, 0, Long.MAX_VALUE);
            final Response<String> scheduledCounterResponse = p.get("counter:jobs:" + SCHEDULED);
            final Response<Long> scheduledResponse = p.zcount("queue:jobs:" + SCHEDULED, 0, Long.MAX_VALUE);
            final Response<String> enqueuedCounterResponse = p.get("counter:jobs:" + ENQUEUED);
            final Response<Long> enqueuedResponse = p.zcount("queue:jobs:" + ENQUEUED, 0, Long.MAX_VALUE);
            final Response<String> processingCounterResponse = p.get("counter:jobs:" + PROCESSING);
            final Response<Long> processingResponse = p.zcount("queue:jobs:" + PROCESSING, 0, Long.MAX_VALUE);
            final Response<String> succeededCounterResponse = p.get("counter:jobs:" + SUCCEEDED);
            final Response<Long> succeededResponse = p.zcount("queue:jobs:" + SUCCEEDED, 0, Long.MAX_VALUE);
            final Response<String> failedCounterResponse = p.get("counter:jobs:" + FAILED);
            final Response<Long> failedResponse = p.zcount("queue:jobs:" + FAILED, 0, Long.MAX_VALUE);

            p.sync();

            final Long awaitingCount = waitingResponse.get() + Long.parseLong(waitingCounterResponse.get() != null ? waitingCounterResponse.get() : "0");
            final Long scheduledCount = scheduledResponse.get() + Long.parseLong(scheduledCounterResponse.get() != null ? scheduledCounterResponse.get() : "0");
            final Long enqueuedCount = enqueuedResponse.get() + Long.parseLong(enqueuedCounterResponse.get() != null ? enqueuedCounterResponse.get() : "0");
            final Long processingCount = processingResponse.get() + Long.parseLong(processingCounterResponse.get() != null ? processingCounterResponse.get() : "0");
            final Long succeededCount = succeededResponse.get() + Long.parseLong(succeededCounterResponse.get() != null ? succeededCounterResponse.get() : "0");
            final Long failedCount = failedResponse.get() + Long.parseLong(failedCounterResponse.get() != null ? failedCounterResponse.get() : "0");
            final Long total = scheduledCount + enqueuedCount;
            return new JobStats(
                    total,
                    awaitingCount,
                    scheduledCount,
                    enqueuedCount,
                    processingCount,
                    failedCount,
                    succeededCount,
                    1
            );
        }
    }

    @Override
    public void publishJobStatCounter(StateName state, int amount) {
        try (Jedis jedis = getJedis()) {
            jedis.incrBy("counter:jobs:" + state, amount);
        }
    }

    protected Jedis getJedis() {
        return jedis;
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

    private void notifyOnChangeListenersIf(boolean mustNotify) {
        if (mustNotify) {
            notifyOnChangeListeners();
        }
    }

    private void notifyOnChangeListeners() {
        if (onChangeListeners.isEmpty()) return;
        if (changeListenerNotificationRateLimit.isRateLimited()) return;

        JobStats jobStats = getJobStats();
        onChangeListeners.forEach(listener -> listener.onChange(jobStats));
    }

    private void saveJob(RedisPipeline p, Job jobToSave) {
        deleteJobMetadata(p, jobToSave);
        p.set(jobVersionKey(jobToSave), String.valueOf(jobToSave.getVersion()));
        p.set(jobKey(jobToSave), jobMapper.serializeJob(jobToSave));
        p.zadd("queue:jobs:" + jobToSave.getState(), toMicroSeconds(jobToSave.getUpdatedAt()), jobToSave.getId().toString());
        p.sadd("job:jobdetails:" + jobToSave.getState(), getJobSignature(jobToSave.getJobDetails()));
        if (SCHEDULED.equals(jobToSave.getState())) {
            p.zadd("queue:scheduledjobs", toMicroSeconds(((ScheduledState) jobToSave.getJobState()).getScheduledAt()), jobToSave.getId().toString());
        }
    }

    private void deleteJobMetadata(RedisPipeline p, Job job) {
        String id = job.getId().toString();
        p.zrem("queue:jobs:" + AWAITING, id);
        p.zrem("queue:jobs:" + SCHEDULED, id);
        p.zrem("queue:jobs:" + ENQUEUED, id);
        p.zrem("queue:jobs:" + PROCESSING, id);
        p.zrem("queue:jobs:" + FAILED, id);
        p.zrem("queue:jobs:" + SUCCEEDED, id);
        p.zrem("queue:jobs:" + DELETED, id);
        p.zrem("queue:scheduledjobs", id);

        p.srem("job:jobdetails:" + AWAITING, getJobSignature(job.getJobDetails()));
        p.srem("job:jobdetails:" + SCHEDULED, getJobSignature(job.getJobDetails()));
        p.srem("job:jobdetails:" + ENQUEUED, getJobSignature(job.getJobDetails()));
        p.srem("job:jobdetails:" + PROCESSING, getJobSignature(job.getJobDetails()));
        p.srem("job:jobdetails:" + FAILED, getJobSignature(job.getJobDetails()));
        p.srem("job:jobdetails:" + SUCCEEDED, getJobSignature(job.getJobDetails()));
        p.srem("job:jobdetails:" + DELETED, getJobSignature(job.getJobDetails()));
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

    private String jobVersionKey(Job job) {
        return jobVersionKey(job.getId());
    }

    private String jobVersionKey(UUID id) {
        return jobKey(id) + ":version";
    }

    private String jobVersionKey(String id) {
        return jobKey(id) + ":version";
    }

    private long toMicroSeconds(Instant instant) {
        return ChronoUnit.MICROS.between(Instant.EPOCH, instant);
    }
}
