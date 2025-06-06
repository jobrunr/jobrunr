package org.jobrunr.storage.sql.common;

import org.jobrunr.storage.JobStats;
import org.jobrunr.storage.StorageProviderUtils;
import org.jobrunr.storage.sql.common.db.Dialect;
import org.jobrunr.storage.sql.common.db.Sql;
import org.jobrunr.storage.sql.common.db.SqlResultSet;

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
                .select("* from jobrunr_jobs_stats")
                .map(resultSet -> toJobStats(resultSet, instant))
                .findFirst()
                .orElse(JobStats.empty()); //why: because oracle returns nothing
    }

    private JobStats toJobStats(SqlResultSet resultSet, Instant instant) {
        return new JobStats(
                instant,
                resultSet.asLong(StorageProviderUtils.JobStats.FIELD_TOTAL),
                resultSet.asLong(StorageProviderUtils.JobStats.FIELD_AWAITING),
                resultSet.asLong(StorageProviderUtils.JobStats.FIELD_SCHEDULED),
                resultSet.asLong(StorageProviderUtils.JobStats.FIELD_ENQUEUED),
                resultSet.asLong(StorageProviderUtils.JobStats.FIELD_PROCESSING),
                resultSet.asLong(StorageProviderUtils.JobStats.FIELD_FAILED),
                resultSet.asLong(StorageProviderUtils.JobStats.FIELD_SUCCEEDED),
                resultSet.asLong(StorageProviderUtils.JobStats.FIELD_ALL_TIME_SUCCEEDED),
                resultSet.asLong(StorageProviderUtils.JobStats.FIELD_DELETED),
                resultSet.asInt(StorageProviderUtils.JobStats.FIELD_NUMBER_OF_RECURRING_JOBS),
                resultSet.asInt(StorageProviderUtils.JobStats.FIELD_NUMBER_OF_BACKGROUND_JOB_SERVERS)
        );
    }
}
