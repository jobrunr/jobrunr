package org.jobrunr.storage.sql;

import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderTest;
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory;
import org.jobrunr.storage.sql.common.db.Sql;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.JdbcDatabaseContainer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.storage.StorageProviderUtils.DatabaseOptions.SKIP_CREATE;
import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

    @Override
    public void cleanup(int testMethodIndex) {
        cleanupDatabase(getDataSource(), testMethodIndex);
    }

    @Override
    protected StorageProvider getStorageProvider() {
        final StorageProvider storageProvider = SqlStorageProviderFactory.using(getDataSource());
        storageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
        setInternalState(storageProvider, "changeListenerNotificationRateLimit", rateLimit().withoutLimits());
        return storageProvider;
    }

    @Test
    void validateTablesDoesNotThrowAnExceptionIfNoTablePrefixIsGiven() {
        assertThatCode(() -> storageProvider.setUpStorageProvider(SKIP_CREATE)).doesNotThrowAnyException();
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

    public static class ThrowingSqlStorageProvider extends ThrowingStorageProvider {

        public ThrowingSqlStorageProvider(StorageProvider storageProvider) {
            super(storageProvider, "dataSource");
        }

        @Override
        protected void makeStorageProviderThrowException(StorageProvider storageProvider) throws SQLException {
            DataSource dataSource = mock(DataSource.class);
            Connection connection = mock(Connection.class);
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString(), eq(ResultSet.TYPE_FORWARD_ONLY), eq(ResultSet.CONCUR_READ_ONLY))).thenThrow(new SQLException("whoopsie"));
            setInternalState(storageProvider, "dataSource", dataSource);
        }
    }
}
