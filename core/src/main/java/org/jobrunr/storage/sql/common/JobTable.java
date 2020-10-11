package org.jobrunr.storage.sql.common;

import org.jobrunr.jobs.AbstractJob;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.jobrunr.storage.PageRequest;
import org.jobrunr.storage.StorageException;
import org.jobrunr.storage.sql.common.db.ConcurrentSqlModificationException;
import org.jobrunr.storage.sql.common.db.Sql;
import org.jobrunr.storage.sql.common.db.SqlResultSet;
import org.jobrunr.utils.JobUtils;

import javax.sql.DataSource;
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
import static org.jobrunr.storage.StorageProviderUtils.areNewJobs;
import static org.jobrunr.storage.StorageProviderUtils.notAllJobsAreExisting;
import static org.jobrunr.storage.StorageProviderUtils.notAllJobsAreNew;
import static org.jobrunr.utils.JobUtils.getJobSignature;
import static org.jobrunr.utils.reflection.ReflectionUtils.cast;

public class JobTable extends Sql<Job> {

    private final DataSource dataSource;
    private final JobMapper jobMapper;
    private static final SqlPageRequestMapper pageRequestMapper = new SqlPageRequestMapper();

    public JobTable(DataSource dataSource, JobMapper jobMapper) {
        this.dataSource = dataSource;
        this.jobMapper = jobMapper;
        this
                .using(dataSource)
                .withVersion(AbstractJob::getVersion)
                .with("jobAsJson", jobMapper::serializeJob)
                .with("jobSignature", JobUtils::getJobSignature)
                .with("scheduledAt", job -> job.hasState(StateName.SCHEDULED) ? job.<ScheduledState>getJobState().getScheduledAt() : null)
                .with("recurringJobId", job -> job.hasState(StateName.SCHEDULED) ? job.<ScheduledState>getJobState().getRecurringJobId() : null);
    }

    public JobTable withId(UUID id) {
        with("id", id);
        return this;
    }

    public JobTable withState(StateName state) {
        with("state", state);
        return this;
    }

    public JobTable withPriority(int priority) {
        with("priority", priority);
        return this;
    }

    public JobTable withAwaitingOn(UUID awaitingOn) {
        with("awaitingOn", awaitingOn);
        return this;
    }

    public JobTable withScheduledAt(Instant scheduledBefore) {
        with("scheduledAt", scheduledBefore);
        return this;
    }

    public JobTable withUpdatedBefore(Instant updatedBefore) {
        with("updatedBefore", updatedBefore);
        return this;
    }

    public Job save(Job jobToSave) {
        boolean isNewJob = jobToSave.getId() == null;
        if (isNewJob) {
            jobToSave.setId(UUID.randomUUID());
            insertOneJob(jobToSave);
        } else {
            jobToSave.increaseVersion();
            try {
                updateOneJob(jobToSave);
            } catch (ConcurrentSqlModificationException e) {
                throw new ConcurrentJobModificationException(jobToSave);
            }
        }
        return jobToSave;
    }

    public List<Job> save(List<Job> jobs) {
        if (jobs.isEmpty()) return jobs;

        if (areNewJobs(jobs)) {
            if (notAllJobsAreNew(jobs)) {
                throw new IllegalArgumentException("All jobs must be either new (with id == null) or existing (with id != null)");
            }
            insertAllJobs(jobs);
        } else {
            try {
                if (notAllJobsAreExisting(jobs)) {
                    throw new IllegalArgumentException("All jobs must be either new (with id == null) or existing (with id != null)");
                }
                updateAllJobs(jobs);
            } catch (ConcurrentSqlModificationException e) {
                List<Job> concurrentUpdatedJobs = cast(e.getFailedItems());
                throw new ConcurrentJobModificationException(concurrentUpdatedJobs);
            }
        }
        return jobs;
    }

    public Optional<Job> selectJobById(UUID id) {
        return withId(id)
                .selectJobs("jobAsJson from jobrunr_jobs where id = :id")
                .findFirst();
    }

    public long countJobs(StateName state) {
        return withState(state)
                .selectCount("from jobrunr_jobs where state = :state");
    }

    public long countJobs(StateName state, int queuePriority) {
        return withState(state)
                .withPriority(queuePriority)
                .selectCount("from jobrunr_jobs where state = :state and priority = :priority");
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
                .map(resultSet -> resultSet.asString("jobSignature"))
                .collect(Collectors.toSet());
    }

    public boolean exists(JobDetails jobDetails, StateName... states) {
        return with("jobSignature", getJobSignature(jobDetails))
                .selectExists("from jobrunr_jobs where state in (" + stream(states).map(stateName -> "'" + stateName.name() + "'").collect(joining(",")) + ") AND jobSignature = :jobSignature");
    }

    public boolean recurringJobExists(String recurringJobId, StateName... states) {
        return with("recurringJobId", recurringJobId)
                .selectExists("from jobrunr_jobs where state in (" + stream(states).map(stateName -> "'" + stateName.name() + "'").collect(joining(",")) + ") AND recurringJobId = :recurringJobId");
    }

    public int deletePermanently(UUID... ids) {
        return delete("from jobrunr_jobs where id in (" + stream(ids).map(uuid -> "'" + uuid.toString() + "'").collect(joining(",")) + ")");
    }

    public int deleteJobsByStateAndUpdatedBefore(StateName state, Instant updatedBefore) {
        return withState(state)
                .withUpdatedBefore(updatedBefore)
                .delete("from jobrunr_jobs where state = :state AND updatedAt <= :updatedBefore");
    }

    @Override
    public JobTable withOrderLimitAndOffset(String order, int limit, long offset) {
        super.withOrderLimitAndOffset(order, limit, offset);
        return this;
    }

    void insertOneJob(Job jobToSave) {
        try (Connection conn = dataSource.getConnection()) {
            insert(conn, jobToSave, "into jobrunr_jobs values (:id, :version, :jobAsJson, :jobSignature, :state, :createdAt, :updatedAt, :scheduledAt, :recurringJobId)");
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    void updateOneJob(Job jobToSave) {
        try (Connection conn = dataSource.getConnection()) {
            update(conn, jobToSave, "jobrunr_jobs SET version = :version, jobAsJson = :jobAsJson, state = :state, updatedAt =:updatedAt, scheduledAt = :scheduledAt WHERE id = :id and version = :previousVersion");
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    void insertAllJobs(List<Job> jobs) {
        jobs.forEach(JobTable::setId);
        insertAll(jobs, "into jobrunr_jobs values (:id, :version, :jobAsJson, :jobSignature, :state, :createdAt, :updatedAt, :scheduledAt, :recurringJobId)");
    }

    void updateAllJobs(List<Job> jobs) {
        jobs.forEach(AbstractJob::increaseVersion);
        updateAll(jobs, "jobrunr_jobs SET version = :version, jobAsJson = :jobAsJson, state = :state, updatedAt =:updatedAt, scheduledAt = :scheduledAt WHERE id = :id and version = :previousVersion");
    }

    private Stream<Job> selectJobs(String statement) {
        final Stream<SqlResultSet> select = super.select(statement);
        return select.map(this::toJob);
    }

    private Job toJob(SqlResultSet resultSet) {
        return jobMapper.deserializeJob(resultSet.asString("jobAsJson"));
    }

    private static void setId(Job job) {
        if (job.getId() == null) {
            job.setId(UUID.randomUUID());
        }
    }
}
