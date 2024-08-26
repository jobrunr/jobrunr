package org.jobrunr.storage.sql.common;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.filters.JobFilterUtils;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.AbstractStorageProvider;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.jobrunr.storage.JobNotFoundException;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.JobStats;
import org.jobrunr.storage.RecurringJobsResult;
import org.jobrunr.storage.StorageException;
import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.storage.StorageProviderUtils.RecurringJobs;
import org.jobrunr.storage.navigation.AmountRequest;
import org.jobrunr.storage.sql.SqlStorageProvider;
import org.jobrunr.storage.sql.common.db.Dialect;
import org.jobrunr.storage.sql.common.db.Transaction;
import org.jobrunr.utils.resilience.RateLimiter;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.jobrunr.jobs.states.StateName.PROCESSING;
import static org.jobrunr.storage.StorageProviderUtils.DatabaseOptions.CREATE;
import static org.jobrunr.storage.StorageProviderUtils.DatabaseOptions.SKIP_CREATE;
import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;
import static org.jobrunr.utils.resilience.RateLimiter.SECOND;

public class DefaultSqlStorageProvider extends AbstractStorageProvider implements SqlStorageProvider {

    protected final DataSource dataSource;
    protected final Dialect dialect;
    protected final String tablePrefix;
    private JobMapper jobMapper;

    public DefaultSqlStorageProvider(DataSource dataSource, Dialect dialect, DatabaseOptions databaseOptions) {
        this(dataSource, dialect, databaseOptions, rateLimit().at1Request().per(SECOND));
    }

    public DefaultSqlStorageProvider(DataSource dataSource, Dialect dialect, String tablePrefix, DatabaseOptions databaseOptions) {
        this(dataSource, dialect, tablePrefix, databaseOptions, rateLimit().at1Request().per(SECOND));
    }

    public DefaultSqlStorageProvider(DataSource dataSource, Dialect dialect, DatabaseOptions databaseOptions, RateLimiter changeListenerNotificationRateLimit) {
        this(dataSource, dialect, null, databaseOptions, changeListenerNotificationRateLimit);
    }

    DefaultSqlStorageProvider(DataSource dataSource, Dialect dialect, String tablePrefix, DatabaseOptions databaseOptions, RateLimiter changeListenerNotificationRateLimit) {
        super(changeListenerNotificationRateLimit);
        this.dataSource = dataSource;
        this.dialect = dialect;
        this.tablePrefix = tablePrefix;
        setUpStorageProvider(databaseOptions);
    }

    @Override
    public void setJobMapper(JobMapper jobMapper) {
        this.jobMapper = jobMapper;
    }

    @Override
    public void setUpStorageProvider(DatabaseOptions databaseOptions) {
        if (databaseOptions == CREATE) {
            getDatabaseCreator()
                    .runMigrations();
        } else if (databaseOptions == SKIP_CREATE) {
            getDatabaseCreator()
                    .validateTables();
        }
    }

    @Override
    public void announceBackgroundJobServer(BackgroundJobServerStatus serverStatus) {
        try (final Connection conn = dataSource.getConnection(); final Transaction transaction = new Transaction(conn)) {
            backgroundJobServerTable(conn).announce(serverStatus);
            transaction.commit();
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public boolean signalBackgroundJobServerAlive(BackgroundJobServerStatus serverStatus) {
        try (final Connection conn = dataSource.getConnection(); final Transaction transaction = new Transaction(conn)) {
            final boolean isServerAlive = backgroundJobServerTable(conn).signalServerAlive(serverStatus);
            transaction.commit();
            return isServerAlive;
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void signalBackgroundJobServerStopped(BackgroundJobServerStatus serverStatus) {
        try (final Connection conn = dataSource.getConnection(); final Transaction transaction = new Transaction(conn)) {
            backgroundJobServerTable(conn).signalServerStopped(serverStatus);
            transaction.commit();
        } catch (SQLException e) {
            throw new StorageException(e);
        }

    }

    @Override
    public List<BackgroundJobServerStatus> getBackgroundJobServers() {
        try (final Connection conn = dataSource.getConnection()) {
            return backgroundJobServerTable(conn).getAll();
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public UUID getLongestRunningBackgroundJobServerId() {
        try (final Connection conn = dataSource.getConnection()) {
            return backgroundJobServerTable(conn).getLongestRunningBackgroundJobServerId();
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public int removeTimedOutBackgroundJobServers(Instant heartbeatOlderThan) {
        try (final Connection conn = dataSource.getConnection(); final Transaction transaction = new Transaction(conn)) {
            final int deletedBackgroundJobServers = backgroundJobServerTable(conn).removeAllWithLastHeartbeatOlderThan(heartbeatOlderThan);
            transaction.commit();
            return deletedBackgroundJobServers;
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void saveMetadata(JobRunrMetadata metadata) {
        try (final Connection conn = dataSource.getConnection(); final Transaction transaction = new Transaction(conn)) {
            metadataTable(conn).save(metadata);
            transaction.commit();
            notifyMetadataChangeListeners();
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public List<JobRunrMetadata> getMetadata(String name) {
        try (final Connection conn = dataSource.getConnection()) {
            return metadataTable(conn).getAll(name);
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public JobRunrMetadata getMetadata(String name, String owner) {
        try (final Connection conn = dataSource.getConnection()) {
            return metadataTable(conn).get(name, owner);
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void deleteMetadata(String name) {
        try (final Connection conn = dataSource.getConnection(); final Transaction transaction = new Transaction(conn)) {
            final int amountDeleted = metadataTable(conn).deleteByName(name);
            transaction.commit();
            notifyMetadataChangeListeners(amountDeleted > 0);
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public Job save(Job jobToSave) {
        try (final Connection conn = dataSource.getConnection(); final Transaction transaction = new Transaction(conn)) {
            final Job savedJob = jobTable(conn).save(jobToSave);
            transaction.commit();
            notifyJobStatsOnChangeListeners();
            return savedJob;
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public List<Job> save(List<Job> jobs) {
        try (final Connection conn = dataSource.getConnection(); final Transaction transaction = new Transaction(conn)) {
            try {
                final List<Job> savedJobs = jobTable(conn).save(jobs);
                transaction.commit();
                notifyJobStatsOnChangeListenersIf(!jobs.isEmpty());
                return savedJobs;
            } catch (ConcurrentJobModificationException e) {
                // even in case of a ConcurrentJobModificationException, we still want to commit the jobs that were saved successfully
                // to be compatible with NoSQL databases
                transaction.commit();
                throw e;
            }
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public Job getJobById(UUID id) {
        try (final Connection conn = dataSource.getConnection()) {
            return jobTable(conn)
                    .selectJobById(id)
                    .orElseThrow(() -> new JobNotFoundException(id));
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public long countJobs(StateName state) {
        try (final Connection conn = dataSource.getConnection()) {
            return jobTable(conn).countJobs(state);
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public List<Job> getJobList(StateName state, Instant updatedBefore, AmountRequest amountRequest) {
        try (final Connection conn = dataSource.getConnection()) {
            return jobTable(conn).selectJobsByState(state, updatedBefore, amountRequest);
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public List<Job> getJobList(StateName state, AmountRequest amountRequest) {
        try (final Connection conn = dataSource.getConnection()) {
            return jobTable(conn).selectJobsByState(state, amountRequest);
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public List<Job> getScheduledJobs(Instant scheduledBefore, AmountRequest amountRequest) {
        try (final Connection conn = dataSource.getConnection()) {
            return jobTable(conn).selectJobsScheduledBefore(scheduledBefore, amountRequest);
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public List<Job> getJobsToProcess(BackgroundJobServer backgroundJobServer, AmountRequest amountRequest) {
        JobFilterUtils jobFilterUtils = new JobFilterUtils(backgroundJobServer.getJobFilters());
        try (final Connection conn = dataSource.getConnection(); final Transaction transaction = new Transaction(conn)) {
            List<Job> jobs = jobTable(conn).selectJobsToProcess(amountRequest);
            try {
                jobs.forEach(job -> job.startProcessingOn(backgroundJobServer));
                jobFilterUtils.runOnStateElectionFilter(jobs);
                List<Job> jobsToProcess = jobTable(conn).save(jobs);
                transaction.commit();
                jobFilterUtils.runOnStateAppliedFilters(jobsToProcess);
                return jobsToProcess.stream().filter(job -> job.hasState(PROCESSING)).collect(toList());
            } catch (ConcurrentJobModificationException e) {
                List<Job> actualSavedJobs = new ArrayList<>(jobs);
                Set<UUID> concurrentUpdatedJobIds = e.getConcurrentUpdatedJobs().stream().map(Job::getId).collect(toSet());
                actualSavedJobs.removeIf(j -> concurrentUpdatedJobIds.contains(j.getId()));
                transaction.commit();
                jobFilterUtils.runOnStateAppliedFilters(actualSavedJobs);
                return actualSavedJobs.stream().filter(job -> job.hasState(PROCESSING)).collect(toList());
            }
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public int deletePermanently(UUID id) {
        try (final Connection conn = dataSource.getConnection(); final Transaction transaction = new Transaction(conn)) {
            final int amountDeleted = jobTable(conn).deletePermanently(id);
            transaction.commit();
            notifyJobStatsOnChangeListenersIf(amountDeleted > 0);
            return amountDeleted;
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public int deleteJobsPermanently(StateName state, Instant updatedBefore) {
        try (final Connection conn = dataSource.getConnection(); final Transaction transaction = new Transaction(conn)) {
            final int amountDeleted = jobTable(conn).deleteJobsByStateAndUpdatedBefore(state, updatedBefore);
            transaction.commit();
            notifyJobStatsOnChangeListenersIf(amountDeleted > 0);
            return amountDeleted;
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public Set<String> getDistinctJobSignatures(StateName... states) {
        try (final Connection conn = dataSource.getConnection()) {
            return jobTable(conn).getDistinctJobSignatures(states);
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    @Deprecated
    public boolean recurringJobExists(String recurringJobId, StateName... states) {
        try (final Connection conn = dataSource.getConnection()) {
            return jobTable(conn).recurringJobExists(recurringJobId, states);
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public Map<String, Instant> getRecurringJobsLatestScheduledRun() {
        try (final Connection conn = dataSource.getConnection()) {
            return jobTable(conn).getRecurringJobsLatestScheduledRun();
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public RecurringJob saveRecurringJob(RecurringJob recurringJob) {
        try (final Connection conn = dataSource.getConnection(); final Transaction transaction = new Transaction(conn)) {
            final RecurringJob savedRecurringJob = recurringJobTable(conn).save(recurringJob);
            transaction.commit();
            return savedRecurringJob;
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public RecurringJobsResult getRecurringJobs() {
        try (final Connection conn = dataSource.getConnection()) {
            return new RecurringJobsResult(recurringJobTable(conn).selectAll());
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public boolean recurringJobsUpdated(Long recurringJobsUpdatedHash) {
        try (final Connection conn = dataSource.getConnection()) {
            Long lastModifiedHash = recurringJobTable(conn).selectSum(RecurringJobs.FIELD_CREATED_AT);
            return !recurringJobsUpdatedHash.equals(lastModifiedHash);
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public int deleteRecurringJob(String id) {
        try (final Connection conn = dataSource.getConnection(); final Transaction transaction = new Transaction(conn)) {
            final int deletedRecurringJobCount = recurringJobTable(conn).deleteById(id);
            transaction.commit();
            return deletedRecurringJobCount;
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public JobStats getJobStats() {
        try (final Connection conn = dataSource.getConnection()) {
            return jobStatsView(conn).getJobStats();
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void publishTotalAmountOfSucceededJobs(int amount) {
        try (final Connection conn = dataSource.getConnection(); final Transaction transaction = new Transaction(conn)) {
            metadataTable(conn).incrementCounter("succeeded-jobs-counter-cluster", amount);
            transaction.commit();
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    protected DatabaseCreator getDatabaseCreator() {
        return new DatabaseCreator(dataSource, tablePrefix, getClass());
    }

    protected JobTable jobTable(Connection connection) {
        return new JobTable(connection, dialect, tablePrefix, jobMapper);
    }

    protected RecurringJobTable recurringJobTable(Connection connection) {
        return new RecurringJobTable(connection, dialect, tablePrefix, jobMapper);
    }

    protected BackgroundJobServerTable backgroundJobServerTable(Connection connection) {
        return new BackgroundJobServerTable(connection, dialect, tablePrefix);
    }

    protected MetadataTable metadataTable(Connection connection) {
        return new MetadataTable(connection, dialect, tablePrefix);
    }

    protected JobStatsView jobStatsView(Connection connection) {
        return new JobStatsView(connection, dialect, tablePrefix);
    }
}
