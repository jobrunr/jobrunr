package org.jobrunr.storage.nosql.redis;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.utils.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static org.jobrunr.storage.StorageProviderUtils.Metadata.NAME;

public class RedisUtilities {

    private RedisUtilities() {

    }

    /**
     * @deprecated: still in use for Migrations
     */
    @Deprecated
    public static String jobCounterKey(StateName stateName) {
        return "counter:jobs:" + stateName;
    }

    public static String backgroundJobServersCreatedKey(String keyPrefix) {
        return toRedisKey(keyPrefix, "backgroundjobservers", "created");
    }

    public static String backgroundJobServersUpdatedKey(String keyPrefix) {
        return toRedisKey(keyPrefix, "backgroundjobservers", "updated");
    }

    public static String backgroundJobServerKey(String keyPrefix, BackgroundJobServerStatus serverStatus) {
        return backgroundJobServerKey(keyPrefix, serverStatus.getId());
    }

    public static String backgroundJobServerKey(String keyPrefix, UUID serverId) {
        return backgroundJobServerKey(keyPrefix, serverId.toString());
    }

    public static String backgroundJobServerKey(String keyPrefix, String serverId) {
        return toRedisKey(keyPrefix, "backgroundjobserver", serverId);
    }

    public static String metadatasKey(String keyPrefix) {
        return toRedisKey(keyPrefix, "set", NAME);
    }

    public static String metadataKey(String keyPrefix, JobRunrMetadata metadata) {
        return toRedisKey(keyPrefix, NAME, metadata.getId());
    }

    public static String metadataKey(String keyPrefix, String metadataId) {
        return toRedisKey(keyPrefix, NAME, metadataId);
    }

    public static String jobQueueForStateKey(String keyPrefix, StateName stateName) {
        return toRedisKey(keyPrefix, "queue", "jobs", stateName.toString());
    }

    public static String recurringJobsKey(String keyPrefix) {
        return toRedisKey(keyPrefix, "recurringjobs");
    }

    public static String recurringJobKey(String keyPrefix, String id) {
        return toRedisKey(keyPrefix, "recurringjob", id);
    }

    public static String scheduledJobsKey(String keyPrefix) {
        return toRedisKey(keyPrefix, "queue", "scheduledjobs");
    }

    public static String jobKey(String keyPrefix, Job job) {
        return jobKey(keyPrefix, job.getId());
    }

    public static String jobKey(String keyPrefix, UUID id) {
        return jobKey(keyPrefix, id.toString());
    }

    public static String jobKey(String keyPrefix, String id) {
        return toRedisKey(keyPrefix, "job", id);
    }

    public static String jobDetailsKey(String keyPrefix, StateName stateName) {
        return toRedisKey(keyPrefix, "job", "jobdetails", stateName.toString());
    }

    public static String recurringJobKey(String keyPrefix, StateName stateName) {
        return toRedisKey(keyPrefix, "job", "recurring-job-id", stateName.toString());
    }

    public static String jobVersionKey(String keyPrefix, Job job) {
        return jobVersionKey(keyPrefix, job.getId());
    }

    public static String jobVersionKey(String keyPrefix, UUID id) {
        return toRedisKey(jobKey(keyPrefix, id), "version");
    }

    private static String toRedisKey(String... keyParts) {
        return Stream.of(keyParts)
                .filter(StringUtils::isNotNullOrEmpty)
                .collect(joining(":"));
    }

    public static long toMicroSeconds(Instant instant) {
        return ChronoUnit.MICROS.between(Instant.EPOCH, instant);
    }
}

