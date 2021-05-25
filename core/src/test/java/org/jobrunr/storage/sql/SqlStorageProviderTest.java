package org.jobrunr.storage.sql;

import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderTest;
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory;
import org.jobrunr.storage.sql.common.db.Transaction;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.mockito.internal.util.reflection.Whitebox;
import org.testcontainers.containers.JdbcDatabaseContainer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;

import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;

public abstract class SqlStorageProviderTest extends StorageProviderTest {

    @Override
    public void cleanup() {
        cleanupDatabase(getDataSource());
    }

    @Override
    protected StorageProvider getStorageProvider() {
        final StorageProvider storageProvider = SqlStorageProviderFactory.using(getDataSource());
        storageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
        Whitebox.setInternalState(storageProvider, "changeListenerNotificationRateLimit", rateLimit().withoutLimits());
        return storageProvider;
    }

    protected static void printSqlContainerDetails(JdbcDatabaseContainer<?> sqlContainer, Duration duration) {
        System.out.println("=========================================================");
        System.out.println(" java version: " + System.getProperty("java.version"));
        System.out.println("   connection: " + sqlContainer.getJdbcUrl());
        System.out.println("         user: " + sqlContainer.getUsername());
        System.out.println("     password: " + sqlContainer.getPassword());
        System.out.println(" startup time: " + duration.getSeconds());
        System.out.println("=========================================================");
    }

    protected abstract DataSource getDataSource();

    protected void cleanupDatabase(DataSource dataSource) {
        drop(dataSource, "view jobrunr_jobs_stats");
        drop(dataSource, "table jobrunr_recurring_jobs");
        drop(dataSource, "table jobrunr_job_counters");
        drop(dataSource, "table jobrunr_jobs");
        drop(dataSource, "table jobrunr_backgroundjobservers");
        drop(dataSource, "table jobrunr_migrations");
        drop(dataSource, "table jobrunr_metadata");
    }

    private void drop(DataSource dataSource, String name) {
        try (final Connection connection = dataSource.getConnection();
             final Transaction tran = new Transaction(connection);
             final Statement statement = connection.createStatement()) {
            statement.executeUpdate("drop " + name);
            System.out.println("Dropped " + name);
            tran.commit();
        } catch (SQLException e) {
            if (canNotIgnoreException(e)) {
                System.out.println("Error dropping " + name);
                e.printStackTrace();
            }
        }
    }

    private boolean canNotIgnoreException(SQLException e) {
        return !canIgnoreException(e);
    }

    protected boolean canIgnoreException(SQLException e) {
        return true;
    }
}
