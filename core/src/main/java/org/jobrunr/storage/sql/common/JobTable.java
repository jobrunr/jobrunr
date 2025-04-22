package org.jobrunr.storage.sql.common;

import org.jobrunr.jobs.AbstractJob;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobListVersioner;
import org.jobrunr.jobs.JobVersioner;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.jobrunr.storage.navigation.AmountRequest;
import org.jobrunr.storage.sql.common.db.ConcurrentSqlModificationException;
import org.jobrunr.storage.sql.common.db.Dialect;
import org.jobrunr.storage.sql.common.db.Sql;
import org.jobrunr.storage.sql.common.db.SqlResultSet;
import org.jobrunr.storage.sql.common.mapper.SqlJobPageRequestMapper;
import org.jobrunr.utils.JobUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.storage.StorageProviderUtils.Jobs.FIELD_CREATED_AT;
import static org.jobrunr.storage.StorageProviderUtils.Jobs.FIELD_ID;
import static org.jobrunr.storage.StorageProviderUtils.Jobs.FIELD_JOB_AS_JSON;
import static org.jobrunr.storage.StorageProviderUtils.Jobs.FIELD_JOB_SIGNATURE;
import static org.jobrunr.storage.StorageProviderUtils.Jobs.FIELD_RECURRING_JOB_ID;
import static org.jobrunr.storage.StorageProviderUtils.Jobs.FIELD_SCHEDULED_AT;
import static org.jobrunr.storage.StorageProviderUtils.Jobs.FIELD_STATE;
import static org.jobrunr.storage.StorageProviderUtils.Jobs.FIELD_UPDATED_AT;
import static org.jobrunr.utils.CollectionUtils.asSet;
import static org.jobrunr.utils.reflection.ReflectionUtils.cast;

public class JobTable extends Sql<Job> {

    private final JobMapper jobMapper;
    private final SqlJobPageRequestMapper pageRequestMapper;

    public JobTable(Connection connection, Dialect dialect, String tablePrefix, JobMapper jobMapper) {
        this.pageRequestMapper = new SqlJobPageRequestMapper(this, dialect);
        this.jobMapper = jobMapper;
        this
                .using(connection, dialect, tablePrefix, "jobrunr_jobs")
                .withVersion(AbstractJob::getVersion)
                .with(FIELD_JOB_AS_JSON, jobMapper::serializeJob)
                .with(FIELD_JOB_SIGNATURE, JobUtils::getJobSignature)
                .with(FIELD_SCHEDULED_AT, job -> job.hasState(StateName.SCHEDULED) ? job.<ScheduledState>getJobState().getScheduledAt() : null)
                .with(FIELD_RECURRING_JOB_ID, job -> job.getRecurringJobId().orElse(null));
    }

    public JobTable withId(UUID id) {
        with(FIELD_ID, id);
        return this;
    }

    public JobTable withState(StateName state) {
        with(FIELD_STATE, state);
        return this;
    }

    public JobTable withScheduledAt(Instant scheduledBefore) {
        with(FIELD_SCHEDULED_AT, scheduledBefore);
        return this;
    }

    public JobTable withUpdatedBefore(Instant updatedBefore) {
        with("updatedBefore", updatedBefore);
        return this;
    }

    public JobTable with(String columnName, String sqlName, String value) {
        if (asSet(FIELD_CREATED_AT, FIELD_UPDATED_AT, FIELD_SCHEDULED_AT).contains(columnName)) {
            with(sqlName, Instant.parse(value));
        } else {
            with(sqlName, value);
        }
        return this;
    }

    public Job save(Job jobToSave) throws SQLException {
        try (JobVersioner jobVersioner = new JobVersioner(jobToSave)) {
            if (jobVersioner.isNewJob()) {
                insertOneJob(jobToSave);
            } else {
                updateOneJob(jobToSave);
            }
            jobVersioner.commitVersion();
        } catch (ConcurrentSqlModificationException e) {
            throw new ConcurrentJobModificationException(jobToSave, e);
        }
        return jobToSave;
    }

    public List<Job> save(List<Job> jobs) throws SQLException {
        if (jobs.isEmpty()) return jobs;

        try (JobListVersioner jobListVersioner = new JobListVersioner(jobs)) {
            try {
                if (jobListVersioner.areNewJobs()) {
                    insertAllJobs(jobs);
                } else {
                    updateAllJobs(jobs);
                }
                jobListVersioner.commitVersions();
                return jobs;
            } catch (ConcurrentSqlModificationException e) {
                List<Job> concurrentUpdatedJobs = cast(e.getFailedItems());
                jobListVersioner.rollbackVersions(concurrentUpdatedJobs);
                throw new ConcurrentJobModificationException(concurrentUpdatedJobs, e);
            }
        }
    }

    public Optional<Job> selectJobById(UUID id) {
        return withId(id)
                .selectJobs("jobAsJson from jobrunr_jobs where id = :id")
                .findFirst();
    }

    public long countJobs(StateName state) throws SQLException {
        return withState(state)
                .selectCount("from jobrunr_jobs where state = :state");
    }

    public List<Job> selectJobsByState(StateName state, AmountRequest amountRequest) {
        return withState(state)
                .selectJobs("jobAsJson from jobrunr_jobs where state = :state", pageRequestMapper.map(amountRequest))
                .collect(toList());
    }

    public List<Job> selectJobsToProcess(AmountRequest amountRequest) {
        return withState(ENQUEUED)
                .selectJobs("jobAsJson from jobrunr_jobs where state = :state", pageRequestMapper.map(amountRequest) + dialect.selectForUpdateSkipLocked())
                .collect(toList());
    }

    public List<Job> selectJobsByState(StateName state, Instant updatedBefore, AmountRequest amountRequest) {
        return withState(state)
                .withUpdatedBefore(updatedBefore)
                .selectJobs("jobAsJson from jobrunr_jobs where state = :state AND updatedAt <= :updatedBefore", pageRequestMapper.map(amountRequest))
                .collect(toList());
    }

    public List<Job> selectJobsScheduledBefore(Instant scheduledBefore, AmountRequest amountRequest) {
        return withScheduledAt(scheduledBefore)
                .selectJobs("jobAsJson from jobrunr_jobs where state = 'SCHEDULED' and scheduledAt <= :scheduledAt", pageRequestMapper.map(amountRequest))
                .collect(toList());
    }

    public Set<String> getDistinctJobSignatures(StateName[] states) {
        return select("distinct jobSignature from jobrunr_jobs where state in (" + stream(states).map(stateName -> "'" + stateName.name() + "'").collect(joining(",")) + ")")
                .map(resultSet -> resultSet.asString(FIELD_JOB_SIGNATURE))
                .collect(Collectors.toSet());
    }

    public boolean recurringJobExists(String recurringJobId, StateName... states) throws SQLException {
        if (states.length < 1) {
            return with(FIELD_RECURRING_JOB_ID, recurringJobId)
                    .selectExists("from jobrunr_jobs where recurringJobId = :recurringJobId");
        }
        return with(FIELD_RECURRING_JOB_ID, recurringJobId)
                .selectExists("from jobrunr_jobs where state in (" + stream(states).map(stateName -> "'" + stateName.name() + "'").collect(joining(",")) + ") AND recurringJobId = :recurringJobId");
    }
    
    public int deletePermanently(UUID... ids) throws SQLException {
        return delete("from jobrunr_jobs where id in (" + stream(ids).map(uuid -> "'" + uuid.toString() + "'").collect(joining(",")) + ")");
    }

    public int deleteJobsByStateAndUpdatedBefore(StateName state, Instant updatedBefore) throws SQLException {
        return withState(state)
                .withUpdatedBefore(updatedBefore)
                .delete("from jobrunr_jobs where state = :state AND updatedAt <= :updatedBefore");
    }

    void insertOneJob(Job jobToSave) throws SQLException {
        insert(jobToSave, "into jobrunr_jobs values (:id, :version, :jobAsJson, :jobSignature, :state, :createdAt, :updatedAt, :scheduledAt, :recurringJobId)");
    }

    void updateOneJob(Job jobToSave) throws SQLException {
        update(jobToSave, "jobrunr_jobs SET version = :version, jobAsJson = :jobAsJson, state = :state, updatedAt =:updatedAt, scheduledAt = :scheduledAt WHERE id = :id and version = :previousVersion");
    }

    void insertAllJobs(List<Job> jobs) throws SQLException {
        insertAll(jobs, "into jobrunr_jobs values (:id, :version, :jobAsJson, :jobSignature, :state, :createdAt, :updatedAt, :scheduledAt, :recurringJobId)");
    }

    void updateAllJobs(List<Job> jobs) throws SQLException {
        updateAll(jobs, "jobrunr_jobs SET version = :version, jobAsJson = :jobAsJson, state = :state, updatedAt =:updatedAt, scheduledAt = :scheduledAt WHERE id = :id and version = :previousVersion");
    }

    private Stream<Job> selectJobs(String statement) {
        return selectJobs(statement, "");
    }

    private Stream<Job> selectJobs(String statement, String filter) {
        final Stream<SqlResultSet> select = super.select(statement, filter);
        return select.map(this::toJob);
    }

    // private Stream<Job> selectJobs(String statement, String filter) {
    //     final Stream<SqlResultSet> select = super.select(statement, filter);
    //     return select
    //         .map(this::toJob)
    //         .filter(Objects::nonNull);  // Important: filter out bad jobs
    // }

    // private Job toJob(SqlResultSet resultSet) {
    //     return jobMapper.deserializeJob(resultSet.asString("jobAsJson"));
    // }

    private Job toJob(SqlResultSet resultSet) {
        String jobAsJson = "UNKNOWN";
        try {
            Class.forName("org.jobrunr.jobs.states.ScheduledState");
            System.out.println("✅ ScheduledState is loadable at runtime!");
        } catch (ClassNotFoundException e) {
            System.err.println("🚨 ScheduledState is NOT loadable at runtime! Classpath problem.");
            e.printStackTrace();
        }
        
        try {
            jobAsJson = resultSet.asString("jobAsJson");
            return jobMapper.deserializeJob(jobAsJson);
        } catch (Exception e) {
            System.err.println("🚨 Failed to deserialize Job with ID [" + jobAsJson + "] due to: " + e.getMessage());
            e.printStackTrace();
            return null; // will be filtered later
        }
    }
    
}
