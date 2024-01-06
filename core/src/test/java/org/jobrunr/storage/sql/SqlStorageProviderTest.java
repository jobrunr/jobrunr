package org.jobrunr.storage.sql;

import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderTest;
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory;
import org.jobrunr.storage.sql.common.db.Sql;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.JdbcDatabaseContainer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Map;

import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.getInternalState;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

public abstract class SqlStorageProviderTest extends StorageProviderTest {

    @BeforeAll
    static void clearParsedStatementCache() {
        final Map<?, ?> parsedStatementCache = getInternalState(Sql.forType(null), "parsedStatementCache");
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
        setInternalState(storageProvider, "changeListenerNotificationRateLimit", rateLimit().withoutLimits());
        return storageProvider;
    }

    @Override
    protected ThrowingStorageProvider makeThrowingStorageProvider(StorageProvider storageProvider) {
        return new ThrowingSqlStorageProvider(storageProvider);
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

    public static class ThrowingSqlStorageProvider extends ThrowingStorageProvider {

        public ThrowingSqlStorageProvider(StorageProvider storageProvider) {
            super(storageProvider, "dataSource");
        }

        @Override
        protected void makeStorageProviderThrowException(StorageProvider storageProvider) throws SQLException {
            DataSource dataSource = mock(DataSource.class);
            Connection connection = mock(Connection.class);
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenThrow(new SQLException("whoopsie"));
            setInternalState(storageProvider, "dataSource", dataSource);
        }
    }
}
