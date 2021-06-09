package org.jobrunr.storage.sql.h2;

import org.h2.jdbcx.JdbcDataSource;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.SqlStorageProviderTest;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.AfterEach;
import org.mockito.internal.util.reflection.Whitebox;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;

public class H2SchemaStorageProviderTest extends SqlStorageProviderTest {

    private static JdbcDataSource dataSource;

    @Override
    protected StorageProvider getStorageProvider() {
        final H2StorageProvider storageProvider = new H2StorageProvider(getDataSource(), "SOME_SCHEMA");
        storageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
        Whitebox.setInternalState(storageProvider, "changeListenerNotificationRateLimit", rateLimit().withoutLimits());
        return storageProvider;
    }

    protected void cleanupDatabase(DataSource dataSource) {
        drop(dataSource, "view SOME_SCHEMA.jobrunr_jobs_stats");
        drop(dataSource, "table SOME_SCHEMA.jobrunr_recurring_jobs");
        drop(dataSource, "table SOME_SCHEMA.jobrunr_job_counters");
        drop(dataSource, "table SOME_SCHEMA.jobrunr_jobs");
        drop(dataSource, "table SOME_SCHEMA.jobrunr_backgroundjobservers");
        drop(dataSource, "table SOME_SCHEMA.jobrunr_migrations");
        drop(dataSource, "table SOME_SCHEMA.jobrunr_metadata");
    }

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = new JdbcDataSource();
            dataSource.setURL("jdbc:h2:/tmp/test-schema;INIT=CREATE SCHEMA IF NOT EXISTS SOME_SCHEMA");
            dataSource.setUser("sa");
            dataSource.setPassword("sa");
        }
        return dataSource;
    }

    @AfterEach
    void checkTablesCreatedInCorrectSchema() throws SQLException {
        try (final Connection connection = dataSource.getConnection(); final Statement statement = connection.createStatement();) {
            try (ResultSet rs = statement.executeQuery("select count(*) from SOME_SCHEMA.jobrunr_jobs")) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                }
            }
        }
    }
}