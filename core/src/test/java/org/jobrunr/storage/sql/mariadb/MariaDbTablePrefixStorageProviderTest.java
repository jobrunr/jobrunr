package org.jobrunr.storage.sql.mariadb;

import org.assertj.core.api.Condition;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderUtils;
import org.jobrunr.storage.sql.DatabaseCleaner;
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.AfterEach;

import javax.sql.DataSource;

import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;

class MariaDbTablePrefixStorageProviderTest extends AbstractMariaDbStorageProviderTest {

    @Override
    protected StorageProvider getStorageProvider() {
        final StorageProvider storageProvider = SqlStorageProviderFactory.using(getDataSource(), "SOME_PREFIX_", StorageProviderUtils.DatabaseOptions.CREATE, rateLimit().withoutLimits());
        storageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
        return storageProvider;
    }

    @Override
    protected DatabaseCleaner getDatabaseCleaner(DataSource dataSource) {
        return new DatabaseCleaner(dataSource, "SOME_PREFIX_");
    }

    @AfterEach
    void checkTablesAndIndicesUseCorrectPrefix() {
        assertThat(dataSource)
                .hasTable("SOME_PREFIX_JOBRUNR_MIGRATIONS")
                .hasTable("SOME_PREFIX_JOBRUNR_JOBS")
                .hasTable("SOME_PREFIX_JOBRUNR_RECURRING_JOBS")
                .hasTable("SOME_PREFIX_JOBRUNR_BACKGROUNDJOBSERVERS")
                .hasTable("SOME_PREFIX_JOBRUNR_METADATA")
                .hasView("SOME_PREFIX_JOBRUNR_JOBS_STATS")
                .hasIndexesMatching(8, new Condition<>(name -> name.startsWith("SOME_PREFIX_JOBRUNR_"), "Index matches"));
    }
}