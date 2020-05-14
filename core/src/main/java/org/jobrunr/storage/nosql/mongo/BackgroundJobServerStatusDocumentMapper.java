package org.jobrunr.storage.nosql.mongo;

import org.bson.Document;
import org.jobrunr.storage.BackgroundJobServerStatus;

import java.util.Date;
import java.util.UUID;

public class BackgroundJobServerStatusDocumentMapper {

    public Document toInsertDocument(BackgroundJobServerStatus serverStatus) {
        final Document document = new Document();
        document.put("_id", serverStatus.getId());
        document.put("workerPoolSize", serverStatus.getWorkerPoolSize());
        document.put("pollIntervalInSeconds", serverStatus.getPollIntervalInSeconds());
        document.put("firstHeartbeat", serverStatus.getFirstHeartbeat());
        document.put("lastHeartbeat", serverStatus.getLastHeartbeat());
        document.put("running", serverStatus.isRunning());
        document.put("systemTotalMemory", serverStatus.getSystemTotalMemory());
        document.put("systemFreeMemory", serverStatus.getSystemFreeMemory());
        document.put("systemCpuLoad", serverStatus.getSystemCpuLoad());
        document.put("processMaxMemory", serverStatus.getProcessMaxMemory());
        document.put("processFreeMemory", serverStatus.getProcessFreeMemory());
        document.put("processAllocatedMemory", serverStatus.getProcessAllocatedMemory());
        document.put("processCpuLoad", serverStatus.getProcessCpuLoad());
        return document;
    }

    public Document toUpdateDocument(BackgroundJobServerStatus serverStatus) {
        final Document document = new Document();
        document.put("lastHeartbeat", serverStatus.getLastHeartbeat());
        document.put("systemFreeMemory", serverStatus.getSystemFreeMemory());
        document.put("systemCpuLoad", serverStatus.getSystemCpuLoad());
        document.put("processFreeMemory", serverStatus.getProcessFreeMemory());
        document.put("processAllocatedMemory", serverStatus.getProcessAllocatedMemory());
        document.put("processCpuLoad", serverStatus.getProcessCpuLoad());

        return new Document("$set", document);
    }

    public BackgroundJobServerStatus toBackgroundJobServerStatus(Document document) {
        return new BackgroundJobServerStatus(
                document.get("_id", UUID.class),
                document.getInteger("workerPoolSize"),
                document.getInteger("pollIntervalInSeconds"),
                document.get("firstHeartbeat", Date.class).toInstant(),
                document.get("lastHeartbeat", Date.class).toInstant(),
                document.getBoolean("running"),
                document.getLong("systemTotalMemory"),
                document.getLong("systemFreeMemory"),
                document.getDouble("systemCpuLoad"),
                document.getLong("processMaxMemory"),
                document.getLong("processFreeMemory"),
                document.getLong("processAllocatedMemory"),
                document.getDouble("processCpuLoad")
        );
    }
}
