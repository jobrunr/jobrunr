package org.jobrunr.storage.sql.common;

import org.jobrunr.jobs.AbstractJob;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.jobrunr.storage.PageRequest;
import org.jobrunr.storage.sql.common.db.ConcurrentSqlModificationException;
import org.jobrunr.storage.sql.common.db.Sql;
import org.jobrunr.storage.sql.common.db.SqlResultSet;
import org.jobrunr.utils.JobUtils;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.jobrunr.utils.JobUtils.getJobSignature;
import static org.jobrunr.utils.reflection.ReflectionUtils.cast;

public class JobTable extends Sql<Job> {

    final JobMapper jobMapper;

    public JobTable(DataSource dataSource, JobMapper jobMapper) {
        this.jobMapper = jobMapper;
        this
                .using(dataSource)
                .withVersion(AbstractJob::getVersion)
                .with("jobAsJson", jobMapper::serializeJob)
                .with("jobSignature", JobUtils::getJobSignature)
                .with("scheduledAt", job -> job.hasState(StateName.SCHEDULED) ? job.<ScheduledState>getJobState().getScheduledAt() : null);
    }

    public JobTable withId(UUID id) {
        with("id", id);
        return this;
    }

    public JobTable withState(StateName state) {
        with("state", state);
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
            insert(jobToSave, "into jobrunr_jobs values (:id, :version, :jobAsJson, :jobSignature, :state, :createdAt, :updatedAt, :scheduledAt)");
        } else {
            jobToSave.increaseVersion();
            try {
                update(jobToSave, "jobrunr_jobs SET version = :version, jobAsJson = :jobAsJson, state = :state, updatedAt =:updatedAt, scheduledAt = :scheduledAt WHERE id = :id and version = :previousVersion");
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

    public long countJobsByState(StateName state) {
        return withState(state)
                .selectCount("from jobrunr_jobs where state = :state");
    }

    public List<Job> selectJobsByState(StateName state, PageRequest pageRequest) {
        return withState(state)
                .withLimitAndOffset(pageRequest.getLimit(), pageRequest.getOffset())
                .selectJobs("jobAsJson from jobrunr_jobs where state = :state ORDER BY createdAt " + pageRequest.getOrder())
                .collect(toList());
    }

    public List<Job> selectJobsByState(StateName state, Instant updatedBefore, PageRequest pageRequest) {
        return withState(state)
                .withUpdatedBefore(updatedBefore)
                .withLimitAndOffset(pageRequest.getLimit(), pageRequest.getOffset())
                .selectJobs("jobAsJson from jobrunr_jobs where state = :state AND updatedAt <= :updatedBefore ORDER BY createdAt " + pageRequest.getOrder())
                .collect(toList());
    }

    public List<Job> selectJobsScheduledBefore(Instant scheduledBefore, PageRequest pageRequest) {
        return withScheduledAt(scheduledBefore)
                .withLimitAndOffset(pageRequest.getLimit(), pageRequest.getOffset())
                .selectJobs("jobAsJson from jobrunr_jobs where state = 'SCHEDULED' and scheduledAt <= :scheduledAt ORDER BY createdAt")
                .collect(toList());
    }

    public boolean exists(JobDetails jobDetails, StateName state) {
        return withState(state)
                .with("jobSignature", getJobSignature(jobDetails))
                .selectExists("from jobrunr_jobs where state = :state AND jobSignature = :jobSignature");
    }

    public int deleteById(UUID id) {
        return withId(id)
                .delete("from jobrunr_jobs where id = :id");
    }

    public int deleteJobsByStateAndUpdatedBefore(StateName state, Instant updatedBefore) {
        return withState(state)
                .withUpdatedBefore(updatedBefore)
                .delete("from jobrunr_jobs where state = :state AND updatedAt <= :updatedBefore");
    }

    @Override
    public JobTable withLimitAndOffset(int limit, long offset) {
        super.withLimitAndOffset(limit, offset);
        return this;
    }

    void insertAllJobs(List<Job> jobs) {
        jobs.forEach(JobTable::setId);
        insertAll(jobs, "into jobrunr_jobs values (:id, :version, :jobAsJson, :jobSignature, :state, :createdAt, :updatedAt, :scheduledAt)");
    }

    void updateAllJobs(List<Job> jobs) {
        jobs.forEach(AbstractJob::increaseVersion);
        updateAll(jobs, "jobrunr_jobs SET version = :version, jobAsJson = :jobAsJson, state = :state, updatedAt =:updatedAt, scheduledAt = :scheduledAt WHERE id = :id and version = :previousVersion");
    }

    Stream<Job> selectJobs(String statement) {
        final Stream<SqlResultSet> select = super.select(statement);
        return select.map(this::toJob);
    }

    Job toJob(SqlResultSet resultSet) {
        return jobMapper.deserializeJob(resultSet.asString("jobAsJson"));
    }

    static void setId(Job job) {
        if (job.getId() == null) {
            job.setId(UUID.randomUUID());
        }
    }

    static boolean notAllJobsAreNew(List<Job> jobs) {
        return jobs.stream().anyMatch(job -> job.getId() != null);
    }

    static boolean notAllJobsAreExisting(List<Job> jobs) {
        return jobs.stream().anyMatch(job -> job.getId() == null);
    }

    static boolean areNewJobs(List<Job> jobs) {
        return jobs.get(0).getId() == null;
    }

}
