package org.jobrunr.storage.nosql.redis;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.AbstractStorageProvider;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.utils.resilience.RateLimiter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

public abstract class AbstractRedisStorageProvider extends AbstractStorageProvider {

    public AbstractRedisStorageProvider(RateLimiter changeListenerNotificationRateLimit) {
        super(changeListenerNotificationRateLimit);
    }

    protected boolean notAllJobsAreNew(List<Job> jobs) {
        return jobs.stream().anyMatch(job -> job.getId() != null);
    }

    protected boolean notAllJobsAreExisting(List<Job> jobs) {
        return jobs.stream().anyMatch(job -> job.getId() == null);
    }

    protected boolean areNewJobs(List<Job> jobs) {
        return jobs.get(0).getId() == null;
    }

    protected String jobCounterKey(StateName stateName) {
        return "counter:jobs:" + stateName;
    }

    protected String backgroundJobServerKey(BackgroundJobServerStatus serverStatus) {
        return backgroundJobServerKey(serverStatus.getId());
    }

    protected String backgroundJobServerKey(UUID serverId) {
        return backgroundJobServerKey(serverId.toString());
    }

    protected String backgroundJobServerKey(String serverId) {
        return "backgroundjobserver:" + serverId;
    }

    protected String jobQueueForStateKey(StateName stateName) {
        return "queue:jobs:" + stateName;
    }

    protected String recurringJobKey(String id) {
        return "recurringjob:" + id;
    }

    protected String jobKey(Job job) {
        return jobKey(job.getId());
    }

    protected String jobKey(UUID id) {
        return jobKey(id.toString());
    }

    protected String jobKey(String id) {
        return "job:" + id;
    }

    protected String jobDetailsKey(StateName stateName) {
        return "job:jobdetails:" + stateName;
    }

    protected String jobVersionKey(Job job) {
        return jobVersionKey(job.getId());
    }

    protected String jobVersionKey(UUID id) {
        return jobKey(id) + ":version";
    }

    protected long toMicroSeconds(Instant instant) {
        return ChronoUnit.MICROS.between(Instant.EPOCH, instant);
    }
}
