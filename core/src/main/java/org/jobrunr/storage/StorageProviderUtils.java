package org.jobrunr.storage;

import org.jobrunr.jobs.Job;

import java.util.List;

public class StorageProviderUtils {

    private StorageProviderUtils() {
    }

    private static final String FIELD_ID = "id";

    public static boolean notAllJobsAreNew(List<Job> jobs) {
        return jobs.stream().anyMatch(job -> job.getId() != null);
    }

    public static boolean notAllJobsAreExisting(List<Job> jobs) {
        return jobs.stream().anyMatch(job -> job.getId() == null);
    }

    public static boolean areNewJobs(List<Job> jobs) {
        return jobs.get(0).getId() == null;
    }

    public static final class Jobs {
        private Jobs() {
        }

        public static final String NAME = "jobs";
        public static final String FIELD_ID = StorageProviderUtils.FIELD_ID;
        public static final String FIELD_VERSION = "version";
        public static final String FIELD_STATE = "state";
        public static final String FIELD_JOB_AS_JSON = "jobAsJson";
        public static final String FIELD_JOB_SIGNATURE = "jobSignature";
        public static final String FIELD_CREATED_AT = "createdAt";
        public static final String FIELD_UPDATED_AT = "updatedAt";
        public static final String FIELD_SCHEDULED_AT = "scheduledAt";
        public static final String FIELD_RECURRING_JOB_ID = "recurringJobId";
    }

    public static class RecurringJobs {
        private RecurringJobs() {
        }

        public static final String NAME = "recurring_jobs";
        public static final String FIELD_ID = StorageProviderUtils.FIELD_ID;
        public static final String FIELD_VERSION = "version";
        public static final String FIELD_JOB_AS_JSON = "jobAsJson";
    }

    public static final class BackgroundJobServers {
        private BackgroundJobServers() {
        }

        public static final String NAME = "background_job_servers";
        public static final String FIELD_ID = StorageProviderUtils.FIELD_ID;
        public static final String FIELD_WORKER_POOL_SIZE = "workerPoolSize";
        public static final String FIELD_POLL_INTERVAL_IN_SECONDS = "pollIntervalInSeconds";
        public static final String FIELD_FIRST_HEARTBEAT = "firstHeartbeat";
        public static final String FIELD_LAST_HEARTBEAT = "lastHeartbeat";
        public static final String FIELD_IS_RUNNING = "running";
        public static final String FIELD_SYSTEM_TOTAL_MEMORY = "systemTotalMemory";
        public static final String FIELD_SYSTEM_FREE_MEMORY = "systemFreeMemory";
        public static final String FIELD_SYSTEM_CPU_LOAD = "systemCpuLoad";
        public static final String FIELD_PROCESS_MAX_MEMORY = "processMaxMemory";
        public static final String FIELD_PROCESS_FREE_MEMORY = "processFreeMemory";
        public static final String FIELD_PROCESS_ALLOCATED_MEMORY = "processAllocatedMemory";
        public static final String FIELD_PROCESS_CPU_LOAD = "processCpuLoad";
    }

    public static final class JobStats {
        private JobStats() {
        }

        public static final String NAME = "job_stats";
        public static final String FIELD_ID = StorageProviderUtils.FIELD_ID;
        public static final String FIELD_STATS = "stats";

    }

}
