package org.jobrunr.storage.sql.common;

import org.jobrunr.jobs.*;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.jobrunr.storage.PageRequest;
import org.jobrunr.storage.sql.common.db.ConcurrentSqlModificationException;
import org.jobrunr.storage.sql.common.db.Sql;
import org.jobrunr.storage.sql.common.db.SqlResultSet;
import org.jobrunr.storage.sql.common.db.dialect.Dialect;
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

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.jobrunr.storage.StorageProviderUtils.Jobs.*;
import static org.jobrunr.utils.JobUtils.getJobSignature;
import static org.jobrunr.utils.reflection.ReflectionUtils.cast;

public class JobTable extends Sql<Job> {

    private final JobMapper jobMapper;
    private static final SqlPageRequestMapper pageRequestMapper = new SqlPageRequestMapper();

    public JobTable(Connection connection, Dialect dialect, String tablePrefix, JobMapper jobMapper) {
        this.jobMapper = jobMapper;
        this
                .using(connection, dialect, tablePrefix, "jobrunr_jobs")
                .withVersion(AbstractJob::getVersion)
                .with(FIELD_JOB_AS_JSON, jobMapper::serializeJob)
                .with(FIELD_JOB_SIGNATURE, JobUtils::getJobSignature)
                .with(FIELD_SCHEDULED_AT, job -> job.hasState(StateName.SCHEDULED) ? job.<ScheduledState>getJobState().getScheduledAt() : null)
                .with(FIELD_RECURRING_JOB_ID, job -> job.hasState(StateName.SCHEDULED) ? job.<ScheduledState>getJobState().getRecurringJobId() : null);
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

    public Job save(Job jobToSave) throws SQLException {
        try(JobVersioner jobVersioner = new JobVersioner(jobToSave)) {
            if (jobVersioner.isNewJob()) {
                insertOneJob(jobToSave);
            } else {
                updateOneJob(jobToSave);
            }
            jobVersioner.commitVersion();
        } catch (ConcurrentSqlModificationException e) {
            throw new ConcurrentJobModificationException(jobToSave);
        }
        return jobToSave;
    }

    public List<Job> save(List<Job> jobs) throws SQLException {
        if (jobs.isEmpty()) return jobs;

        try(JobListVersioner jobListVersioner = new JobListVersioner(jobs)) {
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
                throw new ConcurrentJobModificationException(concurrentUpdatedJobs);
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

    public List<Job> selectJobsByState(StateName state, PageRequest pageRequest) {
        return withState(state)
                .withOrderLimitAndOffset(pageRequestMapper.map(pageRequest), pageRequest.getLimit(), pageRequest.getOffset())
                .selectJobs("jobAsJson from jobrunr_jobs where state = :state")
                .collect(toList());
    }

    public List<Job> selectJobsByState(StateName state, Instant updatedBefore, PageRequest pageRequest) {
        return withState(state)
                .withUpdatedBefore(updatedBefore)
                .withOrderLimitAndOffset(pageRequestMapper.map(pageRequest), pageRequest.getLimit(), pageRequest.getOffset())
                .selectJobs("jobAsJson from jobrunr_jobs where state = :state AND updatedAt <= :updatedBefore")
                .collect(toList());
    }

    public List<Job> selectJobsScheduledBefore(Instant scheduledBefore, PageRequest pageRequest) {
        return withScheduledAt(scheduledBefore)
                .withOrderLimitAndOffset(pageRequestMapper.map(pageRequest), pageRequest.getLimit(), pageRequest.getOffset())
                .selectJobs("jobAsJson from jobrunr_jobs where state = 'SCHEDULED' and scheduledAt <= :scheduledAt")
                .collect(toList());
    }

    public Set<String> getDistinctJobSignatures(StateName[] states) {
        return select("distinct jobSignature from jobrunr_jobs where state in (" + stream(states).map(stateName -> "'" + stateName.name() + "'").collect(joining(",")) + ")")
                .map(resultSet -> resultSet.asString(FIELD_JOB_SIGNATURE))
                .collect(Collectors.toSet());
    }

    public boolean exists(JobDetails jobDetails, StateName... states) throws SQLException {
        return with(FIELD_JOB_SIGNATURE, getJobSignature(jobDetails))
                .selectExists("from jobrunr_jobs where state in (" + stream(states).map(stateName -> "'" + stateName.name() + "'").collect(joining(",")) + ") AND jobSignature = :jobSignature");
    }

    public boolean recurringJobExists(String recurringJobId, StateName... states) throws SQLException {
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

    @Override
    public JobTable withOrderLimitAndOffset(String order, int limit, long offset) {
        super.withOrderLimitAndOffset(order, limit, offset);
        return this;
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
        final Stream<SqlResultSet> select = super.select(statement);
        return select.map(this::toJob);
    }

    private Job toJob(SqlResultSet resultSet) {
        return jobMapper.deserializeJob(resultSet.asString("jobAsJson"));
    }
}
