package org.jobrunr.storage.sql.common;

import org.assertj.core.api.Condition;
import org.jobrunr.storage.sql.SqlStorageProvider;
import org.jobrunr.storage.sql.common.migrations.SqlMigration;
import org.jobrunr.storage.sql.db2.DB2StorageProvider;
import org.jobrunr.storage.sql.oracle.OracleStorageProvider;
import org.jobrunr.storage.sql.sqlserver.SQLServerStorageProvider;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseCreatorTablePrefixTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData databaseMetaData;
    @Mock
    private Statement statement;
    @Mock
    private PreparedStatement preparedStatement;
    @Mock
    private ResultSet resultSet;

    @BeforeEach
    void setUpDatabaseMocks() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getTables(null, null, "%", null)).thenReturn(resultSet);
        when(preparedStatement.executeUpdate()).thenReturn(1);
    }

    @Test
    void testIndexesAreCreatedWithoutSchema() throws SQLException {
        final DatabaseCreator databaseCreator = getDatabaseCreator(dataSource, null, OracleStorageProvider.class);
        databaseCreator.runMigrations();

        assertThat(getAllExecutedStatements())
                .areAtLeastOne(stringContaining("CREATE TABLE jobrunr_jobs"))
                .areAtLeastOne(stringContaining("CREATE INDEX jobrunr_state_idx ON jobrunr_jobs (state)"));
    }

    @Test
    void testIndexesAreCreatedInSchemaForOracle() throws SQLException {
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getDatabaseProductName()).thenReturn("Oracle");

        final DatabaseCreator databaseCreator = getDatabaseCreator(dataSource, "SOME_SCHEMA.", OracleStorageProvider.class);
        databaseCreator.runMigrations();

        assertThat(getAllExecutedStatements())
                .areAtLeastOne(stringContaining("CREATE TABLE SOME_SCHEMA.jobrunr_jobs"))
                .areAtLeastOne(stringContaining("CREATE INDEX SOME_SCHEMA.jobrunr_state_idx ON SOME_SCHEMA.jobrunr_jobs (state)"));
    }

    @Test
    void testIndexesAreCreatedInSchemaForDB2() throws SQLException {
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getDatabaseProductName()).thenReturn("DB2");

        final DatabaseCreator databaseCreator = getDatabaseCreator(dataSource, "SOME_SCHEMA.SOME_PREFIX_", DB2StorageProvider.class);
        databaseCreator.runMigrations();

        assertThat(getAllExecutedStatements())
                .areAtLeastOne(stringContaining("CREATE TABLE SOME_SCHEMA.SOME_PREFIX_jobrunr_jobs"))
                .areAtLeastOne(stringContaining("CREATE INDEX SOME_SCHEMA.SOME_PREFIX_jobrunr_state_idx ON SOME_SCHEMA.SOME_PREFIX_jobrunr_jobs (state)"));
    }

    @Test
    void testIndexesAreCreatedInSchemaForAnsiDatabase() throws SQLException {
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getDatabaseProductName()).thenReturn("SQL Server");

        final DatabaseCreator databaseCreator = getDatabaseCreator(dataSource, "SOME_SCHEMA.SOME_PREFIX_", SQLServerStorageProvider.class);
        databaseCreator.runMigrations();

        assertThat(getAllExecutedStatements())
                .areAtLeastOne(stringContaining("CREATE TABLE SOME_SCHEMA.SOME_PREFIX_jobrunr_jobs"))
                .areAtLeastOne(stringContaining("CREATE INDEX SOME_PREFIX_jobrunr_state_idx ON SOME_SCHEMA.SOME_PREFIX_jobrunr_jobs (state)"))
                .areAtLeastOne(stringContaining("DROP INDEX SOME_PREFIX_jobrunr_job_updated_at_idx ON SOME_SCHEMA.SOME_PREFIX_jobrunr_jobs"));
    }

    @NonNull
    private static DatabaseCreator getDatabaseCreator(DataSource dataSource, String tablePrefix, Class<? extends SqlStorageProvider> sqlStorageProviderClass) {
        return new DatabaseCreator(dataSource, tablePrefix, sqlStorageProviderClass) {
            @Override
            protected boolean isMigrationApplied(SqlMigration migration) {
                return false;
            }
        };
    }

    private Condition<String> stringContaining(String string) {
        return new Condition<>(s -> s.contains(string), "Expected statements to contain " + string);
    }

    private List<String> getAllExecutedStatements() throws SQLException {
        final ArgumentCaptor<String> statementCaptor = ArgumentCaptor.forClass(String.class);
        verify(statement, atLeastOnce()).execute(statementCaptor.capture());
        return statementCaptor.getAllValues();
    }
}
