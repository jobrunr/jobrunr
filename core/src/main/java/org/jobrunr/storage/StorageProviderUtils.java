package org.jobrunr.storage;

import org.jobrunr.jobs.Job;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static org.jobrunr.utils.StringUtils.isNullOrEmpty;

public class StorageProviderUtils {

    private StorageProviderUtils() {
    }

    public static String elementPrefixer(String prefix, String element) {
        return isNullOrEmpty(prefix) ? element : prefix + element;
    }

    public enum DatabaseOptions {
        CREATE,
        SKIP_CREATE
    }

    private static final String FIELD_ID = "id";

    public static final class Migrations {
        private Migrations() {
        }

        public static final String NAME = "migrations";
        public static final String FIELD_ID = StorageProviderUtils.FIELD_ID;
        public static final String FIELD_NAME = "name";
        public static final String FIELD_DATE = "date";
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
        public static final String FIELD_CREATED_AT = "createdAt";
    }

    public static final class BackgroundJobServers {
        private BackgroundJobServers() {
        }

        public static final String NAME = "background_job_servers";
        public static final String FIELD_ID = StorageProviderUtils.FIELD_ID;
        public static final String FIELD_NAME = "name";
        public static final String FIELD_WORKER_POOL_SIZE = "workerPoolSize";
        public static final String FIELD_POLL_INTERVAL_IN_SECONDS = "pollIntervalInSeconds";
        public static final String FIELD_DELETE_SUCCEEDED_JOBS_AFTER = "deleteSucceededJobsAfter";
        public static final String FIELD_PERMANENTLY_DELETE_JOBS_AFTER = "permanentlyDeleteJobsAfter";
        public static final String FIELD_DELETE_DELETED_JOBS_AFTER = "permanentlyDeleteDeletedJobsAfter";
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

    /**
     * @deprecated Is not used anymore in StorageProviders and will be removed
     */
    @Deprecated
    public static final class JobStats {
        private JobStats() {
        }

        public static final String NAME = "job_stats";
        public static final String FIELD_ID = StorageProviderUtils.FIELD_ID;
        public static final String FIELD_STATS = "stats";

    }

    public static final class Metadata {
        private Metadata() {
        }

        public static final String METADATA_OWNER_CLUSTER = "cluster";

        public static final String NAME = "metadata";
        public static final String FIELD_ID = StorageProviderUtils.FIELD_ID;
        public static final String STATS_ID = "succeeded-jobs-counter-cluster";
        public static final String STATS_NAME = "succeeded-jobs-counter";
        public static final String STATS_OWNER = "cluster";
        public static final String FIELD_NAME = "name";
        public static final String FIELD_OWNER = "owner";
        public static final String FIELD_VALUE = "value";
        public static final String FIELD_CREATED_AT = "createdAt";
        public static final String FIELD_UPDATED_AT = "updatedAt";

    }

    public static List<Job> returnConcurrentModifiedJobs(List<Job> jobs, Consumer<Job> consumer) {
        return jobs.stream()
                .map(toConcurrentJobModificationExceptionIfFailed(consumer))
                .filter(Objects::nonNull)
                .map(ConcurrentJobModificationException.class::cast)
                .flatMap(ex -> ex.getConcurrentUpdatedJobs().stream())
                .collect(toList());
    }

    public static Function<Job, Exception> toConcurrentJobModificationExceptionIfFailed(Consumer<Job> test) {
        return job -> {
            try {
                test.accept(job);
                return null;
            } catch (ConcurrentJobModificationException e) {
                return e;
            }
        };
    }
}
