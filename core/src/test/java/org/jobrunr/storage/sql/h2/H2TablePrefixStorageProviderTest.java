package org.jobrunr.storage.sql.h2;

import org.assertj.core.api.Condition;
import org.h2.jdbcx.JdbcDataSource;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.DatabaseCleaner;
import org.jobrunr.storage.sql.SqlStorageProviderTest;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.AfterEach;
import org.mockito.internal.util.reflection.Whitebox;

import javax.sql.DataSource;

import static org.jobrunr.JobRunrAssertions.assertThat;
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

    @Override
    protected DatabaseCleaner getDatabaseCleaner(DataSource dataSource) {
        return new DatabaseCleaner(dataSource, "SOME_SCHEMA.SOME_PREFIX_");
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
    void checkTablesAndIndicesUseCorrectPrefix() {
        assertThat(dataSource)
                .hasTable("SOME_SCHEMA", "SOME_PREFIX_JOBRUNR_MIGRATIONS")
                .hasTable("SOME_SCHEMA", "SOME_PREFIX_JOBRUNR_RECURRING_JOBS")
                .hasTable("SOME_SCHEMA", "SOME_PREFIX_JOBRUNR_BACKGROUNDJOBSERVERS")
                .hasTable("SOME_SCHEMA", "SOME_PREFIX_JOBRUNR_METADATA")
                .hasView("SOME_SCHEMA", "SOME_PREFIX_JOBRUNR_JOBS_STATS")
                .hasIndexesMatching(8, new Condition<>(name -> name.startsWith("SOME_PREFIX_JOBRUNR_"), "Index matches"));
    }
}