package org.jobrunr.storage.sql.db2;

import com.zaxxer.hikari.HikariDataSource;
import org.assertj.core.api.Condition;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.storage.sql.DatabaseCleaner;
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.mockito.internal.util.reflection.Whitebox;

import javax.sql.DataSource;

import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.storage.sql.SqlTestUtils.doInTransaction;
import static org.jobrunr.storage.sql.SqlTestUtils.toHikariDataSource;
import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DB2TablePrefixStorageProviderTest extends AbstractDB2StorageProviderTest {

    private static HikariDataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = toHikariDataSource(sqlContainer);
        }
        return dataSource;
    }

    @AfterAll
    public static void destroyDatasource() {
        dataSource.close();
        dataSource = null;
    }

    @BeforeAll
    void runInitScript() {
        doInTransaction(getDataSource(),
                statement -> statement.execute("create schema SOME_SCHEMA;"),
                "Error creating schema");
    }

    @Override
    protected StorageProvider getStorageProvider() {
        final StorageProvider storageProvider = SqlStorageProviderFactory.using(getDataSource(), "SOME_SCHEMA.SOME_PREFIX_", DatabaseOptions.CREATE);
        storageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
        Whitebox.setInternalState(storageProvider, "changeListenerNotificationRateLimit", rateLimit().withoutLimits());
        return storageProvider;
    }

    @Override
    protected DatabaseCleaner getDatabaseCleaner(DataSource dataSource) {
        return new DatabaseCleaner(dataSource, "SOME_SCHEMA.SOME_PREFIX_");
    }

    @AfterEach
    void checkTablesCreatedWithCorrectPrefix() {
        assertThat(getDataSource())
                .hasTable("SOME_SCHEMA", "SOME_PREFIX_JOBRUNR_MIGRATIONS")
                .hasTable("SOME_SCHEMA", "SOME_PREFIX_JOBRUNR_RECURRING_JOBS")
                .hasTable("SOME_SCHEMA", "SOME_PREFIX_JOBRUNR_BACKGROUNDJOBSERVERS")
                .hasTable("SOME_SCHEMA", "SOME_PREFIX_JOBRUNR_METADATA")
                .hasView("SOME_SCHEMA", "SOME_PREFIX_JOBRUNR_JOBS_STATS")
                .hasIndexesMatching(8, new Condition<>(name -> name.startsWith("SOME_SCHEMA.SOME_PREFIX_JOBRUNR_"), "Index matches"));
    }
}
