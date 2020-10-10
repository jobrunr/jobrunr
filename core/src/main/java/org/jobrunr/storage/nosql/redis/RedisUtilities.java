package org.jobrunr.storage.nosql.redis;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.BackgroundJobServerStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class RedisUtilities {

    private RedisUtilities() {

    }

    public static String jobCounterKey(StateName stateName) {
        return "counter:jobs:" + stateName;
    }

    public static String backgroundJobServerKey(BackgroundJobServerStatus serverStatus) {
        return backgroundJobServerKey(serverStatus.getId());
    }

    public static String backgroundJobServerKey(UUID serverId) {
        return backgroundJobServerKey(serverId.toString());
    }

    public static String backgroundJobServerKey(String serverId) {
        return "backgroundjobserver:" + serverId;
    }

    public static String jobQueueForStateKey(StateName stateName) {
        return "queue:jobs:" + stateName;
    }

    public static String recurringJobKey(String id) {
        return "recurringjob:" + id;
    }

    public static String jobKey(Job job) {
        return jobKey(job.getId());
    }

    public static String jobKey(UUID id) {
        return jobKey(id.toString());
    }

    public static String jobKey(String id) {
        return "job:" + id;
    }

    public static String jobDetailsKey(StateName stateName) {
        return "job:jobdetails:" + stateName;
    }

    public static String recurringJobKey(StateName stateName) {
        return "job:recurring-job-id:" + stateName;
    }

    public static String jobVersionKey(Job job) {
        return jobVersionKey(job.getId());
    }

    public static String jobVersionKey(UUID id) {
        return jobKey(id) + ":version";
    }

    public static long toMicroSeconds(Instant instant) {
        return ChronoUnit.MICROS.between(Instant.EPOCH, instant);
    }
}

