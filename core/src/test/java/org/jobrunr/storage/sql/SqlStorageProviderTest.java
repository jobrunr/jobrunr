package org.jobrunr.storage.sql;

import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderTest;
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.mockito.internal.util.reflection.Whitebox;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

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

    protected abstract DataSource getDataSource();

    protected void cleanupDatabase(DataSource dataSource) {
        drop(dataSource, "view jobrunr_jobs_stats");
        drop(dataSource, "table jobrunr_recurring_jobs");
        drop(dataSource, "table jobrunr_job_counters");
        drop(dataSource, "table jobrunr_jobs");
        drop(dataSource, "table jobrunr_backgroundjobservers");
        drop(dataSource, "table jobrunr_migrations");
    }

    private void drop(DataSource dataSource, String name) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(true);
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("drop " + name);
                System.out.println("Dropped " + name);
            }
        } catch (SQLException e) {
            //e.printStackTrace();
        }
    }
}
