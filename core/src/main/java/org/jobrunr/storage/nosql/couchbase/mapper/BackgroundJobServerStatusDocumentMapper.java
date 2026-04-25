package org.jobrunr.storage.nosql.couchbase.mapper;


import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.MutateInSpec;
import org.jobrunr.storage.BackgroundJobServerStatus;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.FIELD_DELETE_DELETED_JOBS_AFTER;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.FIELD_DELETE_SUCCEEDED_JOBS_AFTER;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.FIELD_FIRST_HEARTBEAT;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.FIELD_ID;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.FIELD_IS_RUNNING;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.FIELD_LAST_HEARTBEAT;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.FIELD_NAME;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.FIELD_POLL_INTERVAL_IN_SECONDS;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.FIELD_PROCESS_ALLOCATED_MEMORY;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.FIELD_PROCESS_CPU_LOAD;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.FIELD_PROCESS_FREE_MEMORY;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.FIELD_PROCESS_MAX_MEMORY;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.FIELD_SYSTEM_CPU_LOAD;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.FIELD_SYSTEM_FREE_MEMORY;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.FIELD_SYSTEM_TOTAL_MEMORY;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.FIELD_WORKER_POOL_SIZE;
import static org.jobrunr.storage.nosql.couchbase.CouchbaseUtils.fromMicroseconds;
import static org.jobrunr.storage.nosql.couchbase.CouchbaseUtils.getIdAsUUID;
import static org.jobrunr.storage.nosql.couchbase.CouchbaseUtils.toCouchbaseId;
import static org.jobrunr.storage.nosql.couchbase.CouchbaseUtils.toMicroSeconds;


public class BackgroundJobServerStatusDocumentMapper {

    public JsonObject toInsertDocument(BackgroundJobServerStatus serverStatus) {
        final JsonObject document = JsonObject.create();
        document.put(toCouchbaseId(FIELD_ID), serverStatus.getId().toString());
        document.put(FIELD_NAME, serverStatus.getName());
        document.put(FIELD_WORKER_POOL_SIZE, serverStatus.getWorkerPoolSize());
        document.put(FIELD_POLL_INTERVAL_IN_SECONDS, serverStatus.getPollIntervalInSeconds());
        document.put(FIELD_DELETE_SUCCEEDED_JOBS_AFTER, serverStatus.getDeleteSucceededJobsAfter().toString());
        document.put(FIELD_DELETE_DELETED_JOBS_AFTER, serverStatus.getPermanentlyDeleteDeletedJobsAfter().toString());
        document.put(FIELD_FIRST_HEARTBEAT, toMicroSeconds(serverStatus.getFirstHeartbeat()));
        document.put(FIELD_LAST_HEARTBEAT, toMicroSeconds(serverStatus.getLastHeartbeat()));
        document.put(FIELD_IS_RUNNING, serverStatus.isRunning());
        document.put(FIELD_SYSTEM_TOTAL_MEMORY, serverStatus.getSystemTotalMemory());
        document.put(FIELD_SYSTEM_FREE_MEMORY, serverStatus.getSystemFreeMemory());
        document.put(FIELD_SYSTEM_CPU_LOAD, serverStatus.getSystemCpuLoad());
        document.put(FIELD_PROCESS_MAX_MEMORY, serverStatus.getProcessMaxMemory());
        document.put(FIELD_PROCESS_FREE_MEMORY, serverStatus.getProcessFreeMemory());
        document.put(FIELD_PROCESS_ALLOCATED_MEMORY, serverStatus.getProcessAllocatedMemory());
        document.put(FIELD_PROCESS_CPU_LOAD, serverStatus.getProcessCpuLoad());
        return document;
    }

    public List<MutateInSpec> toMutateInSpecs(BackgroundJobServerStatus serverStatus) {
        return Arrays.asList(
                MutateInSpec.upsert(FIELD_LAST_HEARTBEAT, toMicroSeconds(serverStatus.getLastHeartbeat())),
                MutateInSpec.upsert(FIELD_SYSTEM_FREE_MEMORY, serverStatus.getSystemFreeMemory()),
                MutateInSpec.upsert(FIELD_SYSTEM_CPU_LOAD, serverStatus.getSystemCpuLoad()),
                MutateInSpec.upsert(FIELD_PROCESS_FREE_MEMORY, serverStatus.getProcessFreeMemory()),
                MutateInSpec.upsert(FIELD_PROCESS_ALLOCATED_MEMORY, serverStatus.getProcessAllocatedMemory()),
                MutateInSpec.upsert(FIELD_PROCESS_CPU_LOAD, serverStatus.getProcessCpuLoad())
        );
    }

    public BackgroundJobServerStatus toBackgroundJobServerStatus(JsonObject document) {
        return new BackgroundJobServerStatus(
                getIdAsUUID(document),
                document.getString(FIELD_NAME),
                document.getInt(FIELD_WORKER_POOL_SIZE),
                document.getInt(FIELD_POLL_INTERVAL_IN_SECONDS),
                Duration.parse(document.getString(FIELD_DELETE_SUCCEEDED_JOBS_AFTER)),
                Duration.parse(document.getString(FIELD_DELETE_DELETED_JOBS_AFTER)),
                fromMicroseconds(document.getLong(FIELD_FIRST_HEARTBEAT)),
                fromMicroseconds(document.getLong(FIELD_LAST_HEARTBEAT)),
                document.getBoolean(FIELD_IS_RUNNING),
                document.getLong(FIELD_SYSTEM_TOTAL_MEMORY),
                document.getLong(FIELD_SYSTEM_FREE_MEMORY),
                document.getDouble(FIELD_SYSTEM_CPU_LOAD),
                document.getLong(FIELD_PROCESS_MAX_MEMORY),
                document.getLong(FIELD_PROCESS_FREE_MEMORY),
                document.getLong(FIELD_PROCESS_ALLOCATED_MEMORY),
                document.getDouble(FIELD_PROCESS_CPU_LOAD));
    }
}
