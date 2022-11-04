package org.jobrunr.storage.nosql.mongo.mapper;

import org.bson.Document;
import org.jobrunr.storage.BackgroundJobServerStatus;

import java.time.Duration;
import java.util.Date;

import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.*;
import static org.jobrunr.storage.nosql.mongo.MongoUtils.getIdAsUUID;

public class BackgroundJobServerStatusDocumentMapper {

    public Document toInsertDocument(BackgroundJobServerStatus serverStatus) {
        final Document document = new Document();
        document.put("_id", serverStatus.getId());
        document.put(FIELD_NAME, serverStatus.getName());
        document.put(FIELD_WORKER_POOL_SIZE, serverStatus.getWorkerPoolSize());
        document.put(FIELD_POLL_INTERVAL_IN_SECONDS, serverStatus.getPollIntervalInSeconds());
        document.put(FIELD_DELETE_SUCCEEDED_JOBS_AFTER, serverStatus.getDeleteSucceededJobsAfter().toString());
        document.put(FIELD_DELETE_DELETED_JOBS_AFTER, serverStatus.getPermanentlyDeleteDeletedJobsAfter().toString());
        document.put(FIELD_FIRST_HEARTBEAT, serverStatus.getFirstHeartbeat());
        document.put(FIELD_LAST_HEARTBEAT, serverStatus.getLastHeartbeat());
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

    public Document toUpdateDocument(BackgroundJobServerStatus serverStatus) {
        final Document document = new Document();
        document.put(FIELD_LAST_HEARTBEAT, serverStatus.getLastHeartbeat());
        document.put(FIELD_SYSTEM_FREE_MEMORY, serverStatus.getSystemFreeMemory());
        document.put(FIELD_SYSTEM_CPU_LOAD, serverStatus.getSystemCpuLoad());
        document.put(FIELD_PROCESS_FREE_MEMORY, serverStatus.getProcessFreeMemory());
        document.put(FIELD_PROCESS_ALLOCATED_MEMORY, serverStatus.getProcessAllocatedMemory());
        document.put(FIELD_PROCESS_CPU_LOAD, serverStatus.getProcessCpuLoad());

        return new Document("$set", document);
    }

    public BackgroundJobServerStatus toBackgroundJobServerStatus(Document document) {

        return new BackgroundJobServerStatus(
                getIdAsUUID(document),
                document.getString(FIELD_NAME),
                document.getInteger(FIELD_WORKER_POOL_SIZE),
                document.getInteger(FIELD_POLL_INTERVAL_IN_SECONDS),
                Duration.parse(document.getString(FIELD_DELETE_SUCCEEDED_JOBS_AFTER)),
                Duration.parse(document.getString(FIELD_DELETE_DELETED_JOBS_AFTER)),
                document.get(FIELD_FIRST_HEARTBEAT, Date.class).toInstant(),
                document.get(FIELD_LAST_HEARTBEAT, Date.class).toInstant(),
                document.getBoolean(FIELD_IS_RUNNING),
                document.getLong(FIELD_SYSTEM_TOTAL_MEMORY),
                document.getLong(FIELD_SYSTEM_FREE_MEMORY),
                document.getDouble(FIELD_SYSTEM_CPU_LOAD),
                document.getLong(FIELD_PROCESS_MAX_MEMORY),
                document.getLong(FIELD_PROCESS_FREE_MEMORY),
                document.getLong(FIELD_PROCESS_ALLOCATED_MEMORY),
                document.getDouble(FIELD_PROCESS_CPU_LOAD)
        );
    }
}
