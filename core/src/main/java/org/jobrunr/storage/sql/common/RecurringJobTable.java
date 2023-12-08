package org.jobrunr.storage.sql.common;

import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.sql.common.db.Dialect;
import org.jobrunr.storage.sql.common.db.Sql;
import org.jobrunr.storage.sql.common.db.SqlResultSet;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.jobrunr.storage.StorageProviderUtils.RecurringJobs.*;

public class RecurringJobTable extends Sql<RecurringJob> {

    private final JobMapper jobMapper;

    public RecurringJobTable(Connection connection, Dialect dialect, String tablePrefix, JobMapper jobMapper) {
        this.jobMapper = jobMapper;
        this
                .using(connection, dialect, tablePrefix, "jobrunr_recurring_jobs")
                .with(FIELD_JOB_AS_JSON, jobMapper::serializeRecurringJob)
                .with(FIELD_CREATED_AT, recurringJob -> recurringJob.getCreatedAt().toEpochMilli());
    }

    public RecurringJobTable withId(String id) {
        with(FIELD_ID, id);
        return this;
    }

    public RecurringJob save(RecurringJob recurringJob) throws SQLException {
        withId(recurringJob.getId());

        if (selectExists("from jobrunr_recurring_jobs where id = :id")) {
            update(recurringJob, "jobrunr_recurring_jobs SET jobAsJson = :jobAsJson, createdAt = :createdAt WHERE id = :id");
        } else {
            insert(recurringJob, "into jobrunr_recurring_jobs values(:id, 1, :jobAsJson, :createdAt)");
        }
        return recurringJob;
    }

    public List<RecurringJob> selectAll() {
        return select("jobAsJson from jobrunr_recurring_jobs")
                .map(this::toRecurringJob)
                .collect(toList());
    }

    public long count() throws SQLException {
        return selectCount("from jobrunr_recurring_jobs");
    }

    public int deleteById(String id) throws SQLException {
        return withId(id)
                .delete("from jobrunr_recurring_jobs where id = :id");
    }

    private RecurringJob toRecurringJob(SqlResultSet resultSet) {
        return jobMapper.deserializeRecurringJob(resultSet.asString(FIELD_JOB_AS_JSON));
    }


}
