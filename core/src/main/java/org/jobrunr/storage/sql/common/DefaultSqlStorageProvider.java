package org.jobrunr.storage.sql.common;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.AbstractStorageProvider;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.JobNotFoundException;
import org.jobrunr.storage.JobStats;
import org.jobrunr.storage.Page;
import org.jobrunr.storage.PageRequest;
import org.jobrunr.storage.sql.SqlStorageProvider;
import org.jobrunr.storage.sql.common.db.Sql;
import org.jobrunr.storage.sql.common.db.SqlResultSet;
import org.jobrunr.utils.resilience.RateLimiter;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;
import static org.jobrunr.utils.resilience.RateLimiter.SECOND;

public class DefaultSqlStorageProvider extends AbstractStorageProvider implements SqlStorageProvider {

    private final DataSource dataSource;

    private JobMapper jobMapper;

    public DefaultSqlStorageProvider(DataSource dataSource) {
        this(dataSource, rateLimit().at2Requests().per(SECOND));
    }

    DefaultSqlStorageProvider(DataSource dataSource, RateLimiter changeListenerNotificationRateLimit) {
        super(changeListenerNotificationRateLimit);
        this.dataSource = dataSource;
        createDBIfNecessary();
    }

    protected void createDBIfNecessary() {
        getDatabaseCreator()
                .runMigrations();
    }

    protected DatabaseCreator getDatabaseCreator() {
        return new DatabaseCreator(dataSource, this);
    }

    @Override
    public void setJobMapper(JobMapper jobMapper) {
        this.jobMapper = jobMapper;
    }

    @Override
    public void announceBackgroundJobServer(BackgroundJobServerStatus serverStatus) {
        backgroundJobServerTable().announce(serverStatus);
    }

    @Override
    public boolean signalBackgroundJobServerAlive(BackgroundJobServerStatus serverStatus) {
        return backgroundJobServerTable().signalServerAlive(serverStatus);
    }

    @Override
    public List<BackgroundJobServerStatus> getBackgroundJobServers() {
        return backgroundJobServerTable()
                .getAll();
    }

    @Override
    public int removeTimedOutBackgroundJobServers(Instant heartbeatOlderThan) {
        return backgroundJobServerTable()
                .removeAllWithLastHeartbeatOlderThan(heartbeatOlderThan);
    }

    @Override
    public Job getJobById(UUID id) {
        return jobTable()
                .selectJobById(id)
                .orElseThrow(() -> new JobNotFoundException(id));
    }

    @Override
    public Job save(Job jobToSave) {
        final Job save = jobTable().save(jobToSave);
        notifyOnChangeListeners();
        return save;
    }

    @Override
    public int delete(UUID id) {
        final int amountDeleted = jobTable().deleteById(id);
        notifyOnChangeListenersIf(amountDeleted > 0);
        return amountDeleted;
    }

    @Override
    public List<Job> save(List<Job> jobs) {
        final List<Job> savedJobs = jobTable().save(jobs);
        notifyOnChangeListeners();
        return savedJobs;
    }

    @Override
    public List<Job> getJobs(StateName state, PageRequest pageRequest) {
        return jobTable()
                .selectJobsByState(state, pageRequest);
    }

    @Override
    public List<Job> getJobs(StateName state, Instant updatedBefore, PageRequest pageRequest) {
        return jobTable()
                .selectJobsByState(state, updatedBefore, pageRequest);
    }

    @Override
    public List<Job> getScheduledJobs(Instant scheduledBefore, PageRequest pageRequest) {
        return jobTable()
                .selectJobsScheduledBefore(scheduledBefore, pageRequest);
    }

    @Override
    public Long countJobs(StateName state) {
        return jobTable()
                .countJobsByState(state);
    }

    @Override
    public Page<Job> getJobPage(StateName state, PageRequest pageRequest) {
        long count = countJobs(state);
        if (count > 0) {
            List<Job> jobs = getJobs(state, pageRequest);
            return new Page<>(count, jobs, pageRequest);
        }
        return new Page<>(0, new ArrayList<>(), pageRequest);
    }

    @Override
    public int deleteJobs(StateName state, Instant updatedBefore) {
        final int amountDeleted = jobTable().deleteJobsByStateAndUpdatedBefore(state, updatedBefore);
        notifyOnChangeListenersIf(amountDeleted > 0);
        return amountDeleted;
    }

    @Override
    public boolean exists(JobDetails jobDetails, StateName state) {
        return jobTable()
                .exists(jobDetails, state);
    }

    @Override
    public RecurringJob saveRecurringJob(RecurringJob recurringJob) {
        return recurringJobTable()
                .save(recurringJob);
    }

    @Override
    public List<RecurringJob> getRecurringJobs() {
        return recurringJobTable()
                .selectAll();
    }

    @Override
    public int deleteRecurringJob(String id) {
        return recurringJobTable()
                .deleteById(id);
    }

    @Override
    public JobStats getJobStats() {
        return Sql.forType(JobStats.class)
                .using(dataSource)
                .select("* from jobrunr_jobs_stats")
                .map(this::toJobStats)
                .findFirst()
                .orElse(JobStats.empty()); //why: because oracle returns nothing
    }

    @Override
    public void publishJobStatCounter(StateName state, int amount) {
        Sql.withoutType()
                .using(dataSource)
                .with("name", state.name())
                .with("amount", amount)
                .update("jobrunr_job_counters set amount = amount + :amount where name = :name");
    }

    private JobStats toJobStats(SqlResultSet resultSet) {
        return new JobStats(
                resultSet.asLong("total"),
                resultSet.asLong("awaiting"),
                resultSet.asLong("scheduled"),
                resultSet.asLong("enqueued"),
                resultSet.asLong("processing"),
                resultSet.asLong("failed"),
                resultSet.asLong("succeeded"),
                resultSet.asInt("nbrOfRecurringJobs"),
                resultSet.asInt("nbrOfBackgroundJobServers")

        );
    }

    private JobTable jobTable() {
        return new JobTable(dataSource, jobMapper);
    }

    private RecurringJobTable recurringJobTable() {
        return new RecurringJobTable(dataSource, jobMapper);
    }

    private BackgroundJobServerTable backgroundJobServerTable() {
        return new BackgroundJobServerTable(dataSource);
    }

}
