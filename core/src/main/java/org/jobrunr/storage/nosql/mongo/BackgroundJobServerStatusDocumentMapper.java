package org.jobrunr.storage.nosql.mongo;

import org.bson.BsonBinarySubType;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.types.Binary;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers;

import java.time.Duration;
import java.util.Date;
import java.util.UUID;

import static org.bson.internal.UuidHelper.decodeBinaryToUuid;
import static org.jobrunr.JobRunrException.shouldNotHappenException;

public class BackgroundJobServerStatusDocumentMapper {

    public Document toInsertDocument(BackgroundJobServerStatus serverStatus) {
        final Document document = new Document();
        document.put("_id", serverStatus.getId());
        document.put(BackgroundJobServers.FIELD_WORKER_POOL_SIZE, serverStatus.getWorkerPoolSize());
        document.put(BackgroundJobServers.FIELD_POLL_INTERVAL_IN_SECONDS, serverStatus.getPollIntervalInSeconds());
        document.put(BackgroundJobServers.FIELD_DELETE_SUCCEEDED_JOBS_AFTER, serverStatus.getDeleteSucceededJobsAfter().toString());
        document.put(BackgroundJobServers.FIELD_DELETE_DELETED_JOBS_AFTER, serverStatus.getPermanentlyDeleteDeletedJobsAfter().toString());
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
                getIdAsUUID(document),
                document.getInteger(BackgroundJobServers.FIELD_WORKER_POOL_SIZE),
                document.getInteger(BackgroundJobServers.FIELD_POLL_INTERVAL_IN_SECONDS),
                Duration.parse(document.getString(BackgroundJobServers.FIELD_DELETE_SUCCEEDED_JOBS_AFTER)),
                Duration.parse(document.getString(BackgroundJobServers.FIELD_DELETE_DELETED_JOBS_AFTER)),
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

    //sorry about this -> necessary to be compatible with MongoDB Java Driver 3 and 4
    //see https://github.com/jobrunr/jobrunr/issues/55
    private UUID getIdAsUUID(Document document) {
        if (document.get("_id") instanceof UUID) {
            return document.get("_id", UUID.class);
        }

        Binary idAsBinary = document.get("_id", Binary.class);
        if (BsonBinarySubType.isUuid(idAsBinary.getType())) {
            if (idAsBinary.getType() == BsonBinarySubType.UUID_STANDARD.getValue()) {
                return decodeBinaryToUuid(clone(idAsBinary.getData()), idAsBinary.getType(), UuidRepresentation.STANDARD);
            } else if (idAsBinary.getType() == BsonBinarySubType.UUID_LEGACY.getValue()) {
                return decodeBinaryToUuid(clone(idAsBinary.getData()), idAsBinary.getType(), UuidRepresentation.JAVA_LEGACY);
            }
        }

        throw shouldNotHappenException("Unknown id: " + document.get("_id").getClass());
    }

    public static byte[] clone(final byte[] array) {
        final byte[] result = new byte[array.length];
        System.arraycopy(array, 0, result, 0, array.length);
        return result;
    }
}
