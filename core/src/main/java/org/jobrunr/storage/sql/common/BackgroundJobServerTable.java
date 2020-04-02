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

    public BackgroundJobServerTable(DataSource dataSource) {
        this
                .using(dataSource)
                .with("id", BackgroundJobServerStatus::getId)
                .with("workerPoolSize", BackgroundJobServerStatus::getWorkerPoolSize)
                .with("pollIntervalInSeconds", BackgroundJobServerStatus::getPollIntervalInSeconds)
                .with("firstHeartbeat", BackgroundJobServerStatus::getFirstHeartbeat)
                .with("running", BackgroundJobServerStatus::isRunning);
    }

    public void announce(BackgroundJobServerStatus serverStatus) {
        this
                .with("lastHeartbeat", Instant.now())
                .insert(serverStatus, "into jobrunr_backgroundjobservers values (:id, :workerPoolSize, :pollIntervalInSeconds, :firstHeartbeat, :lastHeartbeat, :running)");
    }

    public boolean signalServerAlive(BackgroundJobServerStatus serverStatus) {
        try {
            this
                    .with("id", serverStatus.getId())
                    .with("lastHeartbeat", Instant.now())
                    .update("jobrunr_backgroundjobservers SET lastHeartbeat = :lastHeartbeat where id = :id");

            return select("running from jobrunr_backgroundjobservers where id = :id")
                    .map(sqlResultSet -> sqlResultSet.asBoolean("running"))
                    .findFirst()
                    .orElseThrow(() -> new StorageException("Background Job Server with id " + serverStatus.getId() + " is not found"));
        } catch (StorageException e) {
            throw new ServerTimedOutException(serverStatus, e);
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
                resultSet.asUUID("id"),
                resultSet.asInt("workerPoolSize"),
                resultSet.asInt("pollIntervalInSeconds"),
                resultSet.asInstant("firstHeartbeat"),
                resultSet.asInstant("lastHeartbeat"),
                resultSet.asBoolean("running")
        );
    }
}
