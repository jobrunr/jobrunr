package org.jobrunr.storage.sql.common;

import org.jobrunr.storage.JobStats;
import org.jobrunr.storage.sql.common.db.Sql;
import org.jobrunr.storage.sql.common.db.SqlResultSet;
import org.jobrunr.storage.sql.common.db.dialect.Dialect;

import java.sql.Connection;
import java.time.Instant;

public class JobStatsView extends Sql<JobStats> {

    public JobStatsView(Connection connection, Dialect dialect, String tablePrefix) {
        this
                .using(connection, dialect, tablePrefix, "jobrunr_jobs_stats");
    }

    public JobStats getJobStats() {
        Instant instant = Instant.now();
        return this
                .withOrderLimitAndOffset("total ASC", 1, 0)
                .select("* from jobrunr_jobs_stats")
                .map(resultSet -> toJobStats(resultSet, instant))
                .findFirst()
                .orElse(JobStats.empty()); //why: because oracle returns nothing
    }

    private JobStats toJobStats(SqlResultSet resultSet, Instant instant) {
        return new JobStats(
                instant,
                resultSet.asLong("total"),
                resultSet.asLong("scheduled"),
                resultSet.asLong("enqueued"),
                resultSet.asLong("processing"),
                resultSet.asLong("failed"),
                resultSet.asLong("succeeded"),
                resultSet.asLong("allTimeSucceeded"),
                resultSet.asLong("deleted"),
                resultSet.asInt("nbrOfRecurringJobs"),
                resultSet.asInt("nbrOfBackgroundJobServers")
        );
    }
}
