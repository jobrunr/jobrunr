package org.jobrunr.storage.sql;

import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderTest;
import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory;
import org.jobrunr.storage.sql.common.db.Sql;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.JdbcDatabaseContainer;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.storage.StorageProviderUtils.DatabaseOptions.SKIP_CREATE;
import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;
import static org.mockito.internal.util.reflection.Whitebox.getInternalState;

public abstract class SqlStorageProviderTest extends StorageProviderTest {

    @BeforeAll
    static void clearParsedStatementCache() {
        final Map<?, ?> parsedStatementCache = getInternalState(Sql.forType(), "parsedStatementCache");
        parsedStatementCache.clear();
    }

    @Override
    public void cleanup(int testMethodIndex) {
        cleanupDatabase(getDataSource(), testMethodIndex);
    }

    @Override
    protected StorageProvider getStorageProvider() {
        final StorageProvider storageProvider = SqlStorageProviderFactory.using(getDataSource(), null, DatabaseOptions.CREATE, rateLimit().withoutLimits());
        storageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
        return storageProvider;
    }

    @Test
    void validateTablesDoesNotThrowAnExceptionIfNoTablePrefixIsGiven() {
        assertThatCode(() -> storageProvider.setUpStorageProvider(SKIP_CREATE)).doesNotThrowAnyException();
    }

    protected static void printSqlContainerDetails(JdbcDatabaseContainer<?> sqlContainer, Duration duration) {
        System.out.println("=========================================================");
        System.out.println(" java version: " + System.getProperty("java.version"));
        System.out.println("   connection: " + sqlContainer.getJdbcUrl());
        System.out.println("         user: " + sqlContainer.getUsername());
        System.out.println("     password: " + sqlContainer.getPassword());
        System.out.println(" startup time: " + duration.getSeconds());
        System.out.println("=========================================================");
    }

    public abstract DataSource getDataSource();

    protected void cleanupDatabase(DataSource dataSource, int testMethodIndex) {
        if (testMethodIndex < 3) {
            getDatabaseCleaner(dataSource).dropAllTablesAndViews(testMethodIndex);
        } else {
            getDatabaseCleaner(dataSource).deleteAllDataInTables();
        }
    }

    protected DatabaseCleaner getDatabaseCleaner(DataSource dataSource) {
        return new DatabaseCleaner(dataSource);
    }
}
