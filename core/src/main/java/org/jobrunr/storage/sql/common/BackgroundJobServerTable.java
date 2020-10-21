package org.jobrunr.storage.sql.common;

import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.ServerTimedOutException;
import org.jobrunr.storage.StorageException;
import org.jobrunr.storage.sql.common.db.Sql;
import org.jobrunr.storage.sql.common.db.SqlResultSet;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class BackgroundJobServerTable extends Sql<BackgroundJobServerStatus> {

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_WORKER_POOL_SIZE = "workerPoolSize";
    public static final String COLUMN_POLL_INTERVAL_IN_SECONDS = "pollIntervalInSeconds";
    public static final String COLUMN_DELETE_SUCCEEDED_JOBS_AFTER = "deleteSucceededJobsAfter";
    public static final String COLUMN_PERMANENTLY_DELETE_JOBS_AFTER = "permanentlyDeleteJobsAfter";
    public static final String COLUMN_FIRST_HEARTBEAT = "firstHeartbeat";
    public static final String COLUMN_LAST_HEARTBEAT = "lastHeartbeat";
    public static final String COLUMN_RUNNING = "running";
    public static final String COLUMN_SYSTEM_TOTAL_MEMORY = "systemTotalMemory";
    public static final String COLUMN_SYSTEM_FREE_MEMORY = "systemFreeMemory";
    public static final String COLUMN_SYSTEM_CPU_LOAD = "systemCpuLoad";
    public static final String COLUMN_PROCESS_MAX_MEMORY = "processMaxMemory";
    public static final String COLUMN_PROCESS_FREE_MEMORY = "processFreeMemory";
    public static final String COLUMN_PROCESS_ALLOCATED_MEMORY = "processAllocatedMemory";
    public static final String COLUMN_PROCESS_CPU_LOAD = "processCpuLoad";

    public BackgroundJobServerTable(DataSource dataSource) {
        this
                .using(dataSource)
                .with(COLUMN_ID, BackgroundJobServerStatus::getId)
                .with(COLUMN_WORKER_POOL_SIZE, BackgroundJobServerStatus::getWorkerPoolSize)
                .with(COLUMN_POLL_INTERVAL_IN_SECONDS, BackgroundJobServerStatus::getPollIntervalInSeconds)
                .with(COLUMN_DELETE_SUCCEEDED_JOBS_AFTER, BackgroundJobServerStatus::getDeleteSucceededJobsAfter)
                .with(COLUMN_PERMANENTLY_DELETE_JOBS_AFTER, BackgroundJobServerStatus::getPermanentlyDeleteDeletedJobsAfter)
                .with(COLUMN_FIRST_HEARTBEAT, BackgroundJobServerStatus::getFirstHeartbeat)
                .with(COLUMN_LAST_HEARTBEAT, BackgroundJobServerStatus::getLastHeartbeat)
                .with(COLUMN_RUNNING, BackgroundJobServerStatus::isRunning)
                .with(COLUMN_SYSTEM_TOTAL_MEMORY, BackgroundJobServerStatus::getSystemTotalMemory)
                .with(COLUMN_SYSTEM_FREE_MEMORY, BackgroundJobServerStatus::getSystemFreeMemory)
                .with(COLUMN_SYSTEM_CPU_LOAD, BackgroundJobServerStatus::getSystemCpuLoad)
                .with(COLUMN_PROCESS_MAX_MEMORY, BackgroundJobServerStatus::getProcessMaxMemory)
                .with(COLUMN_PROCESS_FREE_MEMORY, BackgroundJobServerStatus::getProcessFreeMemory)
                .with(COLUMN_PROCESS_ALLOCATED_MEMORY, BackgroundJobServerStatus::getProcessAllocatedMemory)
                .with(COLUMN_PROCESS_CPU_LOAD, BackgroundJobServerStatus::getProcessCpuLoad);
    }

    public void announce(BackgroundJobServerStatus serverStatus) {
        this
                .with(COLUMN_ID, serverStatus.getId())
                .delete("from jobrunr_backgroundjobservers where id = :id");
        this
                .insert(serverStatus, "into jobrunr_backgroundjobservers values (:id, :workerPoolSize, :pollIntervalInSeconds, :firstHeartbeat, :lastHeartbeat, :running, :systemTotalMemory, :systemFreeMemory, :systemCpuLoad, :processMaxMemory, :processFreeMemory, :processAllocatedMemory, :processCpuLoad, :deleteSucceededJobsAfter, :permanentlyDeleteJobsAfter)");
    }

    public boolean signalServerAlive(BackgroundJobServerStatus serverStatus) {
        try {
            this
                    .with(COLUMN_ID, serverStatus.getId())
                    .with(COLUMN_LAST_HEARTBEAT, serverStatus.getLastHeartbeat())
                    .with(COLUMN_SYSTEM_FREE_MEMORY, serverStatus.getSystemFreeMemory())
                    .with(COLUMN_SYSTEM_CPU_LOAD, serverStatus.getSystemCpuLoad())
                    .with(COLUMN_PROCESS_FREE_MEMORY, serverStatus.getProcessFreeMemory())
                    .with(COLUMN_PROCESS_ALLOCATED_MEMORY, serverStatus.getProcessAllocatedMemory())
                    .with(COLUMN_PROCESS_CPU_LOAD, serverStatus.getSystemCpuLoad())
                    .update("jobrunr_backgroundjobservers SET lastHeartbeat = :lastHeartbeat, systemFreeMemory = :systemFreeMemory, systemCpuLoad = :systemCpuLoad, processFreeMemory = :processFreeMemory, processAllocatedMemory = :processAllocatedMemory, processCpuLoad = :processCpuLoad where id = :id");

            return select("running from jobrunr_backgroundjobservers where id = :id")
                    .map(sqlResultSet -> sqlResultSet.asBoolean(COLUMN_RUNNING))
                    .findFirst()
                    .orElseThrow(() -> new StorageException("Background Job Server with id " + serverStatus.getId() + " is not found"));
        } catch (StorageException e) {
            throw new ServerTimedOutException(serverStatus, e);
        }
    }

    public void signalServerStopped(BackgroundJobServerStatus serverStatus) {
        try {
            this
                    .with(COLUMN_ID, serverStatus.getId())
                    .delete("from jobrunr_backgroundjobservers where id = :id");
        } catch (StorageException notImportant) {
            // this is not important
        }
    }

    public int removeAllWithLastHeartbeatOlderThan(Instant heartbeatOlderThan) {
        return with("heartbeatOlderThan", heartbeatOlderThan)
                .delete("from jobrunr_backgroundjobservers where lastHeartbeat < :heartbeatOlderThan");
    }

    public List<BackgroundJobServerStatus> getAll() {
        return select("* from jobrunr_backgroundjobservers order by firstHeartbeat")
                .map(this::toBackgroundJobServerStatus)
                .collect(toList());
    }

    private BackgroundJobServerStatus toBackgroundJobServerStatus(SqlResultSet resultSet) {
        return new BackgroundJobServerStatus(
                resultSet.asUUID(COLUMN_ID),
                resultSet.asInt(COLUMN_WORKER_POOL_SIZE),
                resultSet.asInt(COLUMN_POLL_INTERVAL_IN_SECONDS),
                resultSet.asDuration(COLUMN_DELETE_SUCCEEDED_JOBS_AFTER),
                resultSet.asDuration(COLUMN_PERMANENTLY_DELETE_JOBS_AFTER),
                resultSet.asInstant(COLUMN_FIRST_HEARTBEAT),
                resultSet.asInstant(COLUMN_LAST_HEARTBEAT),
                resultSet.asBoolean(COLUMN_RUNNING),
                resultSet.asLong(COLUMN_SYSTEM_TOTAL_MEMORY),
                resultSet.asLong(COLUMN_SYSTEM_FREE_MEMORY),
                resultSet.asDouble(COLUMN_SYSTEM_CPU_LOAD),
                resultSet.asLong(COLUMN_PROCESS_MAX_MEMORY),
                resultSet.asLong(COLUMN_PROCESS_FREE_MEMORY),
                resultSet.asLong(COLUMN_PROCESS_ALLOCATED_MEMORY),
                resultSet.asDouble(COLUMN_PROCESS_CPU_LOAD)
        );
    }

}
