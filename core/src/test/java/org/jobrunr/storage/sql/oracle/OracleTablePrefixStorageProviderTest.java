package org.jobrunr.storage.sql.oracle;

import org.assertj.core.api.Condition;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.storage.sql.DatabaseCleaner;
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.executioncondition.RunTestBetween;
import org.mockito.internal.util.reflection.Whitebox;

import javax.sql.DataSource;

import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@RunTestBetween(from = "00:00", to = "03:00")
public class OracleTablePrefixStorageProviderTest extends AbstractOracleStorageProviderTest {

    @BeforeAll
    void runInitScript() {
        // it's no longer possible to create a user (aka schema) because the container no longer runs with a system user
        // see https://github.com/testcontainers/testcontainers-java/issues/4615
    }

    @Override
    protected StorageProvider getStorageProvider() {
        final StorageProvider storageProvider = SqlStorageProviderFactory.using(getDataSource(), "TEST.SOME_PREFIX_", DatabaseOptions.CREATE);
        storageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
        Whitebox.setInternalState(storageProvider, "changeListenerNotificationRateLimit", rateLimit().withoutLimits());
        return storageProvider;
    }

    @Override
    protected DatabaseCleaner getDatabaseCleaner(DataSource dataSource) {
        return new DatabaseCleaner(dataSource, "TEST.SOME_PREFIX_");
    }

    @AfterEach
    void checkTablesCreatedWithCorrectPrefix() {
        assertThat(dataSource)
                .hasTable("TEST", "SOME_PREFIX_JOBRUNR_MIGRATIONS")
                .hasTable("TEST", "SOME_PREFIX_JOBRUNR_JOBS")
                .hasTable("TEST", "SOME_PREFIX_JOBRUNR_RECURRING_JOBS")
                .hasTable("TEST", "SOME_PREFIX_JOBRUNR_BACKGROUNDJOBSERVERS")
                .hasTable("TEST", "SOME_PREFIX_JOBRUNR_METADATA")
                .hasView("TEST", "SOME_PREFIX_JOBRUNR_JOBS_STATS")
                .hasIndexesMatching(8, new Condition<>(name -> name.startsWith("TEST.SOME_PREFIX_JOBRUNR_"), "Index matches"));
    }
}