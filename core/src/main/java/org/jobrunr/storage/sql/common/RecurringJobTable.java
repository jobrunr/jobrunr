package org.jobrunr.storage.sql.common;

import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.sql.common.db.Sql;
import org.jobrunr.storage.sql.common.db.SqlResultSet;
import org.jobrunr.storage.sql.common.db.dialect.Dialect;

import javax.sql.DataSource;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.jobrunr.storage.StorageProviderUtils.RecurringJobs.FIELD_ID;
import static org.jobrunr.storage.StorageProviderUtils.RecurringJobs.FIELD_JOB_AS_JSON;

public class RecurringJobTable extends Sql<RecurringJob> {

    private final JobMapper jobMapper;

    public RecurringJobTable(DataSource dataSource, Dialect dialect, String schemaName, JobMapper jobMapper) {
        this.jobMapper = jobMapper;
        this
                .using(dataSource, dialect, schemaName, "jobrunr_recurring_jobs")
                .with(FIELD_JOB_AS_JSON, jobMapper::serializeRecurringJob);
    }

    public RecurringJobTable withId(String id) {
        with(FIELD_ID, id);
        return this;
    }

    public RecurringJob save(RecurringJob recurringJob) {
        withId(recurringJob.getId());

        if (selectExists("from jobrunr_recurring_jobs where id = :id")) {
            update(recurringJob, "jobrunr_recurring_jobs SET jobAsJson = :jobAsJson WHERE id = :id");
        } else {
            insert(recurringJob, "into jobrunr_recurring_jobs values(:id, 1, :jobAsJson)");
        }
        return recurringJob;
    }

    public List<RecurringJob> selectAll() {
        return select("jobAsJson from jobrunr_recurring_jobs")
                .map(this::toRecurringJob)
                .collect(toList());
    }

    public int deleteById(String id) {
        return withId(id)
                .delete("from jobrunr_recurring_jobs where id = :id");
    }

    private RecurringJob toRecurringJob(SqlResultSet resultSet) {
        return jobMapper.deserializeRecurringJob(resultSet.asString(FIELD_JOB_AS_JSON));
    }
}
