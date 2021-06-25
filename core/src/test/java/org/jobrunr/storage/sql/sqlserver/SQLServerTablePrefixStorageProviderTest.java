package org.jobrunr.storage.sql.sqlserver;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import org.assertj.core.api.Condition;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.common.DefaultSqlStorageProvider;
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.mockito.internal.util.reflection.Whitebox;
import org.testcontainers.ext.ScriptUtils;
import org.testcontainers.jdbc.JdbcDatabaseDelegate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SQLServerTablePrefixStorageProviderTest extends AbstractSQLServerStorageProviderTest {

    private static SQLServerDataSource dataSource;

    @BeforeAll
    void runInitScript() {
        String packageName = SQLServerTablePrefixStorageProviderTest.class.getPackageName();
        String initScriptPath = packageName.replaceAll("\\.", "/") + "/init_with_schema.sql";
        JdbcDatabaseDelegate containerDelegate = new JdbcDatabaseDelegate(sqlContainer, "");
        ScriptUtils.runInitScript(containerDelegate, initScriptPath);
    }

    @Override
    protected StorageProvider getStorageProvider() {
        final StorageProvider storageProvider = SqlStorageProviderFactory.using(getDataSource(), "SOME_SCHEMA.SOME_PREFIX_", DefaultSqlStorageProvider.DatabaseOptions.CREATE);
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
            dataSource = new SQLServerDataSource();
            dataSource.setURL(sqlContainer.getJdbcUrl());
            dataSource.setUser(sqlContainer.getUsername());
            dataSource.setPassword(sqlContainer.getPassword());
        }
        return dataSource;
    }

    @AfterEach
    void checkTablesCreatedWithCorrectPrefix() throws SQLException {
        try (final Connection connection = dataSource.getConnection(); final Statement statement = connection.createStatement()) {
            try (ResultSet rs = statement.executeQuery("SELECT * FROM INFORMATION_SCHEMA.TABLES")) {
                List<String> allTables = new ArrayList<>();
                while (rs.next()) {
                    String tableCatalog = rs.getString(1);
                    String tableSchema = rs.getString(2);
                    String tableName = rs.getString(3);

                    allTables.add(tableSchema + "." + tableName);
                }

                assertThat(allTables).areAtLeast(5, new Condition<>(name -> name.toUpperCase().startsWith("SOME_SCHEMA.SOME_PREFIX_JOBRUNR_"), ""));
            }

            try (ResultSet rs = statement.executeQuery("SELECT idx.name as indexname FROM sys.indexes idx WHERE idx.name IS NOT NULL")) {
                List<String> allIndices = new ArrayList<>();
                while (rs.next()) {
                    String indexName = rs.getString("indexname");
                    allIndices.add(indexName);
                }

                assertThat(allIndices).areAtLeast(8, new Condition<>(name -> name.toUpperCase().startsWith("SOME_PREFIX_JOBRUNR_"), ""));
            }
        }
    }
}