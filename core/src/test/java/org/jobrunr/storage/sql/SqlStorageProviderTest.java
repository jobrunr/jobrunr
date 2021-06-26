package org.jobrunr.storage.sql;

import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderTest;
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory;
import org.jobrunr.storage.sql.common.db.Sql;
import org.jobrunr.storage.sql.common.db.Transaction;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.internal.util.reflection.Whitebox;
import org.testcontainers.containers.JdbcDatabaseContainer;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.Map;

import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;

public abstract class SqlStorageProviderTest extends StorageProviderTest {

    @BeforeAll
    static void clearParsedStatementCache() {
        final Map<?, ?> parsedStatementCache = Whitebox.getInternalState(Sql.forType(null), "parsedStatementCache");
        parsedStatementCache.clear();
    }

    private static Class<? extends SqlStorageProviderTest> currentTestClass;
    private static int testMethodIndex;

    @Override
    public void cleanup() {
        cleanupDatabase(getDataSource());
    }

    @Override
    protected StorageProvider getStorageProvider() {
        final StorageProvider storageProvider = SqlStorageProviderFactory.using(getDataSource());
        storageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
        Whitebox.setInternalState(storageProvider, "changeListenerNotificationRateLimit", rateLimit().withoutLimits());
        return storageProvider;
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

    protected abstract DataSource getDataSource();

    protected void cleanupDatabase(DataSource dataSource) {
        if (getTestMethodIndex() < 3) {
            getDatabaseCleaner(dataSource).dropAllTablesAndViews();
        } else {
            getDatabaseCleaner(dataSource).deleteAllDataInTables();
        }
    }

    protected DatabaseCleaner getDatabaseCleaner(DataSource dataSource) {
        return new DatabaseCleaner(dataSource);
    }

    private int getTestMethodIndex() {
        if (currentTestClass != this.getClass()) {
            testMethodIndex = 0;
        }
        currentTestClass = this.getClass();
        return testMethodIndex++;
    }
}
