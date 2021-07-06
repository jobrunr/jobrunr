package org.jobrunr.storage.sql.common;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.*;
import org.jobrunr.storage.sql.SqlStorageProvider;
import org.jobrunr.storage.sql.common.db.Transaction;
import org.jobrunr.storage.sql.common.db.dialect.Dialect;
import org.jobrunr.utils.resilience.RateLimiter;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
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

    private final DataSource dataSource;
    private final Dialect dialect;
    private final String tablePrefix;
    private final DatabaseOptions databaseOptions;
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
        return new DatabaseCreator(dataSource, tablePrefix, getClass());
    }

    @Override
    public void setJobMapper(JobMapper jobMapper) {
        this.jobMapper = jobMapper;
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
            final List<Job> savedJobs = jobTable(conn).save(jobs);
            transaction.commit();
            notifyJobStatsOnChangeListenersIf(!jobs.isEmpty());
            return savedJobs;
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
    public List<Job> getScheduledJobs(Instant scheduledBefore, PageRequest pageRequest) {
        try (final Connection conn = dataSource.getConnection()) {
            return jobTable(conn).selectJobsScheduledBefore(scheduledBefore, pageRequest);
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public List<Job> getJobs(StateName state, PageRequest pageRequest) {
        try (final Connection conn = dataSource.getConnection()) {
            return jobTable(conn).selectJobsByState(state, pageRequest);
        } catch (SQLException e) {
            throw new StorageException(e);
        }

    }

    @Override
    public List<Job> getJobs(StateName state, Instant updatedBefore, PageRequest pageRequest) {
        try (final Connection conn = dataSource.getConnection()) {
            return jobTable(conn).selectJobsByState(state, updatedBefore, pageRequest);
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public Page<Job> getJobPage(StateName state, PageRequest pageRequest) {
        try (final Connection conn = dataSource.getConnection()) {
            long count = jobTable(conn).countJobs(state);
            if (count > 0) {
                List<Job> jobs = jobTable(conn).selectJobsByState(state, pageRequest);
                return new Page<>(count, jobs, pageRequest);
            }
            return new Page<>(0, new ArrayList<>(), pageRequest);
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
    public boolean exists(JobDetails jobDetails, StateName... states) {
        try (final Connection conn = dataSource.getConnection()) {
            return jobTable(conn).exists(jobDetails, states);
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public boolean recurringJobExists(String recurringJobId, StateName... states) {
        try (final Connection conn = dataSource.getConnection()) {
            return jobTable(conn).recurringJobExists(recurringJobId, states);
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
    public List<RecurringJob> getRecurringJobs() {
        try (final Connection conn = dataSource.getConnection()) {
            return recurringJobTable(conn).selectAll();
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
