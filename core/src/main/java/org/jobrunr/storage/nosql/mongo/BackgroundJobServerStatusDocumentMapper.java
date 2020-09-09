package org.jobrunr.storage.nosql.mongo;

import org.bson.Document;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers;

import java.util.Date;
import java.util.UUID;

public class BackgroundJobServerStatusDocumentMapper {

    public Document toInsertDocument(BackgroundJobServerStatus serverStatus) {
        final Document document = new Document();
        document.put("_id", serverStatus.getId());
        document.put(BackgroundJobServers.FIELD_WORKER_POOL_SIZE, serverStatus.getWorkerPoolSize());
        document.put(BackgroundJobServers.FIELD_POLL_INTERVAL_IN_SECONDS, serverStatus.getPollIntervalInSeconds());
        document.put(BackgroundJobServers.FIELD_FIRST_HEARTBEAT, serverStatus.getFirstHeartbeat());
        document.put(BackgroundJobServers.FIELD_LAST_HEARTBEAT, serverStatus.getLastHeartbeat());
        document.put(BackgroundJobServers.FIELD_IS_RUNNING, serverStatus.isRunning());
        document.put(BackgroundJobServers.FIELD_SYSTEM_TOTAL_MEMORY, serverStatus.getSystemTotalMemory());
        document.put(BackgroundJobServers.FIELD_SYSTEM_FREE_MEMORY, serverStatus.getSystemFreeMemory());
        document.put(BackgroundJobServers.FIELD_SYSTEM_CPU_LOAD, serverStatus.getSystemCpuLoad());
        document.put(BackgroundJobServers.FIELD_PROCESS_MAX_MEMORY, serverStatus.getProcessMaxMemory());
        document.put(BackgroundJobServers.FIELD_PROCESS_FREE_MEMORY, serverStatus.getProcessFreeMemory());
        document.put(BackgroundJobServers.FIELD_PROCESS_ALLOCATED_MEMORY, serverStatus.getProcessAllocatedMemory());
        document.put(BackgroundJobServers.FIELD_PROCESS_CPU_LOAD, serverStatus.getProcessCpuLoad());
        return document;
    }

    public Document toUpdateDocument(BackgroundJobServerStatus serverStatus) {
        final Document document = new Document();
        document.put(BackgroundJobServers.FIELD_LAST_HEARTBEAT, serverStatus.getLastHeartbeat());
        document.put(BackgroundJobServers.FIELD_SYSTEM_FREE_MEMORY, serverStatus.getSystemFreeMemory());
        document.put(BackgroundJobServers.FIELD_SYSTEM_CPU_LOAD, serverStatus.getSystemCpuLoad());
        document.put(BackgroundJobServers.FIELD_PROCESS_FREE_MEMORY, serverStatus.getProcessFreeMemory());
        document.put(BackgroundJobServers.FIELD_PROCESS_ALLOCATED_MEMORY, serverStatus.getProcessAllocatedMemory());
        document.put(BackgroundJobServers.FIELD_PROCESS_CPU_LOAD, serverStatus.getProcessCpuLoad());

        return new Document("$set", document);
    }

    public BackgroundJobServerStatus toBackgroundJobServerStatus(Document document) {
        return new BackgroundJobServerStatus(
                document.get("_id", UUID.class),
                document.getInteger(BackgroundJobServers.FIELD_WORKER_POOL_SIZE),
                document.getInteger(BackgroundJobServers.FIELD_POLL_INTERVAL_IN_SECONDS),
                document.get(BackgroundJobServers.FIELD_FIRST_HEARTBEAT, Date.class).toInstant(),
                document.get(BackgroundJobServers.FIELD_LAST_HEARTBEAT, Date.class).toInstant(),
                document.getBoolean(BackgroundJobServers.FIELD_IS_RUNNING),
                document.getLong(BackgroundJobServers.FIELD_SYSTEM_TOTAL_MEMORY),
                document.getLong(BackgroundJobServers.FIELD_SYSTEM_FREE_MEMORY),
                document.getDouble(BackgroundJobServers.FIELD_SYSTEM_CPU_LOAD),
                document.getLong(BackgroundJobServers.FIELD_PROCESS_MAX_MEMORY),
                document.getLong(BackgroundJobServers.FIELD_PROCESS_FREE_MEMORY),
                document.getLong(BackgroundJobServers.FIELD_PROCESS_ALLOCATED_MEMORY),
                document.getDouble(BackgroundJobServers.FIELD_PROCESS_CPU_LOAD)
        );
    }
}
