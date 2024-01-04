package org.jobrunr.storage.sql.h2;

import org.assertj.core.api.Condition;
import org.h2.jdbcx.JdbcConnectionPool;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.DatabaseCleaner;
import org.jobrunr.storage.sql.SqlStorageProviderTest;
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.AfterEach;
import org.mockito.internal.util.reflection.Whitebox;

import javax.sql.DataSource;

import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;

public class NativePoolH2TablePrefixStorageProviderTest extends SqlStorageProviderTest {

    private static JdbcConnectionPool dataSource;

    @Override
    protected StorageProvider getStorageProvider() {
        final StorageProvider storageProvider = SqlStorageProviderFactory.using(getDataSource(), "SOME_PREFIX_");
        storageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
        Whitebox.setInternalState(storageProvider, "changeListenerNotificationRateLimit", rateLimit().withoutLimits());
        return storageProvider;
    }

    @Override
    protected DatabaseCleaner getDatabaseCleaner(DataSource dataSource) {
        return new DatabaseCleaner(dataSource, "SOME_PREFIX_");
    }

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            deleteFile("/tmp/test-schema.mv.db");
            deleteFile("/tmp/test-schema.trace.db");
            dataSource = JdbcConnectionPool.create("jdbc:h2:/tmp/test-schema;INIT=CREATE SCHEMA IF NOT EXISTS SOME_SCHEMA", "sa", "as");
        }
        return dataSource;
    }

    @AfterEach
    void checkTablesAndIndicesUseCorrectPrefix() {
        assertThat(dataSource)
                .hasTable("PUBLIC.SOME_PREFIX_JOBRUNR_MIGRATIONS")
                .hasTable("PUBLIC.SOME_PREFIX_JOBRUNR_RECURRING_JOBS")
                .hasTable("PUBLIC.SOME_PREFIX_JOBRUNR_BACKGROUNDJOBSERVERS")
                .hasTable("PUBLIC.SOME_PREFIX_JOBRUNR_METADATA")
                .hasView("PUBLIC.SOME_PREFIX_JOBRUNR_JOBS_STATS")
                .hasIndexesMatching(8, new Condition<>(name -> name.startsWith("SOME_PREFIX_JOBRUNR_"), "Index matches"));
    }
}