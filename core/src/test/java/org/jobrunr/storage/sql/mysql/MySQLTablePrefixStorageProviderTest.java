package org.jobrunr.storage.sql.mysql;

import com.mysql.cj.jdbc.MysqlConnectionPoolDataSource;
import com.mysql.cj.jdbc.MysqlDataSource;
import org.assertj.core.api.Condition;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.DatabaseCleaner;
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.mockito.internal.util.reflection.Whitebox;

import javax.sql.DataSource;

import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;

class MySQLTablePrefixStorageProviderTest extends AbstractMySQLStorageProviderTest {

    private static MysqlDataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = new MysqlConnectionPoolDataSource();
            dataSource.setUrl(sqlContainer.getJdbcUrl() + "?rewriteBatchedStatements=true&pool=true&useSSL=false");
            dataSource.setUser(sqlContainer.getUsername());
            dataSource.setPassword(sqlContainer.getPassword());
        }
        return dataSource;
    }

    @AfterAll
    public static void destroyDatasource() {
        dataSource = null;
    }

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

    @AfterEach
    void checkTablesAndIndicesUseCorrectPrefix() {
        assertThat(dataSource)
                .hasTable("SOME_PREFIX_JOBRUNR_MIGRATIONS")
                .hasTable("SOME_PREFIX_JOBRUNR_RECURRING_JOBS")
                .hasTable("SOME_PREFIX_JOBRUNR_BACKGROUNDJOBSERVERS")
                .hasTable("SOME_PREFIX_JOBRUNR_METADATA")
                .hasView("SOME_PREFIX_JOBRUNR_JOBS_STATS")
                .hasIndexesMatching(8, new Condition<>(name -> name.startsWith("SOME_PREFIX_JOBRUNR_"), "Index matches"));
    }
}