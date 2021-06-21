package org.jobrunr.storage.sql.h2;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;

public class H2TablePrefixStorageProviderTest extends SqlStorageProviderTest {

    private static JdbcDataSource dataSource;

    @Override
    protected StorageProvider getStorageProvider() {
        final H2StorageProvider storageProvider = new H2StorageProvider(getDataSource(), "SOME_SCHEMA.SOME_PREFIX_");
        storageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
        Whitebox.setInternalState(storageProvider, "changeListenerNotificationRateLimit", rateLimit().withoutLimits());
        return storageProvider;
    }

    protected void cleanupDatabase(DataSource dataSource) {
        drop(dataSource, "view SOME_SCHEMA.SOME_PREFIX_jobrunr_jobs_stats");
        drop(dataSource, "table SOME_SCHEMA.SOME_PREFIX_jobrunr_recurring_jobs");
        drop(dataSource, "table SOME_SCHEMA.SOME_PREFIX_jobrunr_job_counters");
        drop(dataSource, "table SOME_SCHEMA.SOME_PREFIX_jobrunr_jobs");
        drop(dataSource, "table SOME_SCHEMA.SOME_PREFIX_jobrunr_backgroundjobservers");
        drop(dataSource, "table SOME_SCHEMA.SOME_PREFIX_jobrunr_migrations");
        drop(dataSource, "table SOME_SCHEMA.SOME_PREFIX_jobrunr_metadata");
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
    void checkTablesCreatedWithCorrectPrefix() throws SQLException {
        try (final Connection connection = dataSource.getConnection(); final Statement statement = connection.createStatement();) {
            try (ResultSet rs = statement.executeQuery("SELECT * FROM INFORMATION_SCHEMA.TABLES")) {
                List<String> allTables = new ArrayList<>();
                while (rs.next()) {
                    String tableCatalog = rs.getString(1);
                    String tableSchema = rs.getString(2);
                    String tableName = rs.getString(3);

                    allTables.add(tableSchema + "." + tableName);
                }

                assertThat(allTables).areAtLeast(5, new Condition<>(name -> name.startsWith("SOME_SCHEMA.SOME_PREFIX_JOBRUNR_"), ""));
            }

            try (ResultSet rs = statement.executeQuery("SELECT * FROM information_schema.indexes")) {
                List<String> allIndices = new ArrayList<>();
                while (rs.next()) {
                    String indexName = rs.getString(5);
                    allIndices.add(indexName);
                }

                assertThat(allIndices).areAtLeast(8, new Condition<>(name -> name.startsWith("SOME_PREFIX_JOBRUNR_"), ""));
            }
        }
    }
}