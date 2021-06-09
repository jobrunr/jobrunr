package org.jobrunr.storage.sql.common;

import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.ServerTimedOutException;
import org.jobrunr.storage.StorageException;
import org.jobrunr.storage.sql.common.db.Sql;
import org.jobrunr.storage.sql.common.db.SqlResultSet;
import org.jobrunr.storage.sql.common.db.dialect.Dialect;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toList;
import static org.jobrunr.JobRunrException.shouldNotHappenException;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.FIELD_DELETE_SUCCEEDED_JOBS_AFTER;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.FIELD_FIRST_HEARTBEAT;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.FIELD_ID;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.FIELD_IS_RUNNING;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.FIELD_LAST_HEARTBEAT;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.FIELD_PERMANENTLY_DELETE_JOBS_AFTER;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.FIELD_POLL_INTERVAL_IN_SECONDS;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.FIELD_PROCESS_ALLOCATED_MEMORY;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.FIELD_PROCESS_CPU_LOAD;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.FIELD_PROCESS_FREE_MEMORY;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.FIELD_PROCESS_MAX_MEMORY;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.FIELD_SYSTEM_CPU_LOAD;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.FIELD_SYSTEM_FREE_MEMORY;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.FIELD_SYSTEM_TOTAL_MEMORY;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.FIELD_WORKER_POOL_SIZE;

public class BackgroundJobServerTable extends Sql<BackgroundJobServerStatus> {

    public BackgroundJobServerTable(DataSource dataSource, Dialect dialect, String schemaName) {
        this
                .using(dataSource, dialect, schemaName, "jobrunr_backgroundjobservers")
                .with(FIELD_ID, BackgroundJobServerStatus::getId)
                .with(FIELD_WORKER_POOL_SIZE, BackgroundJobServerStatus::getWorkerPoolSize)
                .with(FIELD_POLL_INTERVAL_IN_SECONDS, BackgroundJobServerStatus::getPollIntervalInSeconds)
                .with(FIELD_DELETE_SUCCEEDED_JOBS_AFTER, BackgroundJobServerStatus::getDeleteSucceededJobsAfter)
                .with(FIELD_PERMANENTLY_DELETE_JOBS_AFTER, BackgroundJobServerStatus::getPermanentlyDeleteDeletedJobsAfter)
                .with(FIELD_FIRST_HEARTBEAT, BackgroundJobServerStatus::getFirstHeartbeat)
                .with(FIELD_LAST_HEARTBEAT, BackgroundJobServerStatus::getLastHeartbeat)
                .with(FIELD_IS_RUNNING, BackgroundJobServerStatus::isRunning)
                .with(FIELD_SYSTEM_TOTAL_MEMORY, BackgroundJobServerStatus::getSystemTotalMemory)
                .with(FIELD_SYSTEM_FREE_MEMORY, BackgroundJobServerStatus::getSystemFreeMemory)
                .with(FIELD_SYSTEM_CPU_LOAD, BackgroundJobServerStatus::getSystemCpuLoad)
                .with(FIELD_PROCESS_MAX_MEMORY, BackgroundJobServerStatus::getProcessMaxMemory)
                .with(FIELD_PROCESS_FREE_MEMORY, BackgroundJobServerStatus::getProcessFreeMemory)
                .with(FIELD_PROCESS_ALLOCATED_MEMORY, BackgroundJobServerStatus::getProcessAllocatedMemory)
                .with(FIELD_PROCESS_CPU_LOAD, BackgroundJobServerStatus::getProcessCpuLoad);
    }

    public void announce(BackgroundJobServerStatus serverStatus) {
        this
                .with(FIELD_ID, serverStatus.getId())
                .delete("from jobrunr_backgroundjobservers where id = :id");
        this
                .insert(serverStatus, "into jobrunr_backgroundjobservers values (:id, :workerPoolSize, :pollIntervalInSeconds, :firstHeartbeat, :lastHeartbeat, :running, :systemTotalMemory, :systemFreeMemory, :systemCpuLoad, :processMaxMemory, :processFreeMemory, :processAllocatedMemory, :processCpuLoad, :deleteSucceededJobsAfter, :permanentlyDeleteJobsAfter)");
    }

    public boolean signalServerAlive(BackgroundJobServerStatus serverStatus) {
        try {
            this
                    .with(FIELD_ID, serverStatus.getId())
                    .with(FIELD_LAST_HEARTBEAT, serverStatus.getLastHeartbeat())
                    .with(FIELD_SYSTEM_FREE_MEMORY, serverStatus.getSystemFreeMemory())
                    .with(FIELD_SYSTEM_CPU_LOAD, serverStatus.getSystemCpuLoad())
                    .with(FIELD_PROCESS_FREE_MEMORY, serverStatus.getProcessFreeMemory())
                    .with(FIELD_PROCESS_ALLOCATED_MEMORY, serverStatus.getProcessAllocatedMemory())
                    .with(FIELD_PROCESS_CPU_LOAD, serverStatus.getSystemCpuLoad())
                    .update("jobrunr_backgroundjobservers SET lastHeartbeat = :lastHeartbeat, systemFreeMemory = :systemFreeMemory, systemCpuLoad = :systemCpuLoad, processFreeMemory = :processFreeMemory, processAllocatedMemory = :processAllocatedMemory, processCpuLoad = :processCpuLoad where id = :id");

            return select("running from jobrunr_backgroundjobservers where id = :id")
                    .map(sqlResultSet -> sqlResultSet.asBoolean(FIELD_IS_RUNNING))
                    .findFirst()
                    .orElseThrow(() -> new StorageException("Background Job Server with id " + serverStatus.getId() + " is not found"));
        } catch (StorageException e) {
            throw new ServerTimedOutException(serverStatus, e);
        }
    }

    public void signalServerStopped(BackgroundJobServerStatus serverStatus) {
        try {
            this
                    .with(FIELD_ID, serverStatus.getId())
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

    public UUID getLongestRunningBackgroundJobServerId() {
        return withOrderLimitAndOffset("firstHeartbeat ASC", 1, 0)
                .select("id from jobrunr_backgroundjobservers")
                .map(sqlResultSet -> sqlResultSet.asUUID(FIELD_ID))
                .findFirst()
                .orElseThrow(() -> shouldNotHappenException("No servers available?!"));
    }

    private BackgroundJobServerStatus toBackgroundJobServerStatus(SqlResultSet resultSet) {
        return new BackgroundJobServerStatus(
                resultSet.asUUID(FIELD_ID),
                resultSet.asInt(FIELD_WORKER_POOL_SIZE),
                resultSet.asInt(FIELD_POLL_INTERVAL_IN_SECONDS),
                resultSet.asDuration(FIELD_DELETE_SUCCEEDED_JOBS_AFTER),
                resultSet.asDuration(FIELD_PERMANENTLY_DELETE_JOBS_AFTER),
                resultSet.asInstant(FIELD_FIRST_HEARTBEAT),
                resultSet.asInstant(FIELD_LAST_HEARTBEAT),
                resultSet.asBoolean(FIELD_IS_RUNNING),
                resultSet.asLong(FIELD_SYSTEM_TOTAL_MEMORY),
                resultSet.asLong(FIELD_SYSTEM_FREE_MEMORY),
                resultSet.asDouble(FIELD_SYSTEM_CPU_LOAD),
                resultSet.asLong(FIELD_PROCESS_MAX_MEMORY),
                resultSet.asLong(FIELD_PROCESS_FREE_MEMORY),
                resultSet.asLong(FIELD_PROCESS_ALLOCATED_MEMORY),
                resultSet.asDouble(FIELD_PROCESS_CPU_LOAD)
        );
    }


}
