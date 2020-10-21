package org.jobrunr.storage.sql.common;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.*;
import org.jobrunr.storage.sql.SqlStorageProvider;
import org.jobrunr.storage.sql.common.db.Sql;
import org.jobrunr.storage.sql.common.db.SqlResultSet;
import org.jobrunr.utils.resilience.RateLimiter;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.jobrunr.storage.sql.common.DefaultSqlStorageProvider.DatabaseOptions.CREATE;
import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;
import static org.jobrunr.utils.resilience.RateLimiter.SECOND;

public class DefaultSqlStorageProvider extends AbstractStorageProvider implements SqlStorageProvider {

    public enum DatabaseOptions {
        CREATE,
        SKIP_CREATE
    }

    protected final DataSource dataSource;
    protected final DatabaseOptions databaseOptions;
    protected JobMapper jobMapper;

    public DefaultSqlStorageProvider(DataSource dataSource) {
        this(dataSource, CREATE, rateLimit().at1Request().per(SECOND));
    }

    public DefaultSqlStorageProvider(DataSource dataSource, DatabaseOptions databaseOptions) {
        this(dataSource, databaseOptions, rateLimit().at1Request().per(SECOND));
    }

    DefaultSqlStorageProvider(DataSource dataSource, DatabaseOptions databaseOptions, RateLimiter changeListenerNotificationRateLimit) {
        super(changeListenerNotificationRateLimit);
        this.dataSource = dataSource;
        this.databaseOptions = databaseOptions;
        createDBIfNecessary();
    }

    protected void createDBIfNecessary() {
        if (databaseOptions == CREATE) {
            getDatabaseCreator()
                    .runMigrations();
        } else {
            getDatabaseCreator()
                    .validateTables();
        }
    }

    protected DatabaseCreator getDatabaseCreator() {
        return new DatabaseCreator(dataSource, getClass());
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
    public void signalBackgroundJobServerStopped(BackgroundJobServerStatus serverStatus) {
        backgroundJobServerTable().signalServerStopped(serverStatus);
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
        notifyJobStatsOnChangeListeners();
        return save;
    }

    @Override
    public int deletePermanently(UUID id) {
        final int amountDeleted = jobTable().deletePermanently(id);
        notifyJobStatsOnChangeListenersIf(amountDeleted > 0);
        return amountDeleted;
    }

    @Override
    public List<Job> save(List<Job> jobs) {
        final List<Job> savedJobs = jobTable().save(jobs);
        notifyJobStatsOnChangeListenersIf(!jobs.isEmpty());
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
                .countJobs(state);
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
    public int deleteJobsPermanently(StateName state, Instant updatedBefore) {
        final int amountDeleted = jobTable().deleteJobsByStateAndUpdatedBefore(state, updatedBefore);
        notifyJobStatsOnChangeListenersIf(amountDeleted > 0);
        return amountDeleted;
    }

    @Override
    public Set<String> getDistinctJobSignatures(StateName... states) {
        return jobTable()
                .getDistinctJobSignatures(states);
    }

    @Override
    public boolean exists(JobDetails jobDetails, StateName... states) {
        return jobTable()
                .exists(jobDetails, states);
    }

    @Override
    public boolean recurringJobExists(String recurringJobId, StateName... states) {
        return jobTable()
                .recurringJobExists(recurringJobId, states);
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
        Instant instant = Instant.now();
        return Sql.forType(JobStats.class)
                .using(dataSource)
                .withOrderLimitAndOffset("total ASC", 1, 0)
                .select("* from jobrunr_jobs_stats")
                .map(resultSet -> toJobStats(resultSet, instant))
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

    private JobStats toJobStats(SqlResultSet resultSet, Instant instant) {
        return new JobStats(
                instant,
                resultSet.asLong("total"),
                resultSet.asLong("awaiting"),
                resultSet.asLong("scheduled"),
                resultSet.asLong("enqueued"),
                resultSet.asLong("processing"),
                resultSet.asLong("failed"),
                resultSet.asLong("succeeded"),
                resultSet.asLong("deleted"),
                resultSet.asInt("nbrOfRecurringJobs"),
                resultSet.asInt("nbrOfBackgroundJobServers")

        );
    }

    protected JobTable jobTable() {
        return new JobTable(dataSource, jobMapper);
    }

    protected RecurringJobTable recurringJobTable() {
        return new RecurringJobTable(dataSource, jobMapper);
    }

    protected BackgroundJobServerTable backgroundJobServerTable() {
        return new BackgroundJobServerTable(dataSource);
    }

}
