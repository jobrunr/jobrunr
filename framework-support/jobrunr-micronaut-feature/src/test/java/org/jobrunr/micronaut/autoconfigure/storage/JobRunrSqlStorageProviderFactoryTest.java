package org.jobrunr.micronaut.autoconfigure.storage;


import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.SqlStorageProvider;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.*;

import static org.jobrunr.micronaut.MicronautAssertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest(rebuildContext = true)
class JobRunrSqlStorageProviderFactoryTest {

    @Inject
    ApplicationContext context;

    @Test
    @Property(name = "jobrunr.database.skip-create", value = "true")
    void sqlStorageProviderAutoConfigurationTestWithDefaultDataSource() throws SQLException {
        // GIVEN
        context.registerSingleton(dataSource());

        // WHEN & THEN
        assertThat(context).hasSingleBean(StorageProvider.class);
        assertThat(context.getBean(StorageProvider.class))
                .isInstanceOf(SqlStorageProvider.class)
                .hasJobMapper();
        assertThat(context).doesNotHaveBean(InMemoryStorageProvider.class);
    }

    @Test
    @Property(name = "jobrunr.database.skip-create", value = "true")
    @Property(name = "jobrunr.database.datasource", value = "jobrunr")
    void sqlStorageProviderAutoConfigurationTestWithMultipleNamedDataSources() throws SQLException {
        // GIVEN
        context.registerSingleton(dataSource());
        context.registerSingleton(DataSource.class, dataSource(), Qualifiers.byName("jobrunr"));

        // WHEN & THEN
        assertThat(context).hasSingleBean(StorageProvider.class);
        assertThat(context.getBean(StorageProvider.class))
                .isInstanceOf(SqlStorageProvider.class)
                .hasJobMapper();
        assertThat(context).doesNotHaveBean(InMemoryStorageProvider.class);
    }

    public DataSource dataSource() throws SQLException {
        DataSource dataSourceMock = mock(DataSource.class);
        Connection connectionMock = mock(Connection.class);
        DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
        when(dataSourceMock.getConnection()).thenReturn(connectionMock);
        when(connectionMock.getMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getURL()).thenReturn("jdbc:sqlite:this is not important");

        mockTablePresent(connectionMock, "jobrunr_jobs", "jobrunr_recurring_jobs", "jobrunr_backgroundjobservers", "jobrunr_metadata");

        return dataSourceMock;
    }

    private void mockTablePresent(Connection connectionMock, String... tableNames) throws SQLException {
        Statement statementMock = mock(Statement.class);
        when(connectionMock.createStatement()).thenReturn(statementMock);
        for (String tableName : tableNames) {
            ResultSet resultSetMock = mock(ResultSet.class);
            when(statementMock.executeQuery("select count(*) from " + tableName)).thenReturn(resultSetMock);
            when(resultSetMock.next()).thenReturn(true);
            when(resultSetMock.getInt(1)).thenReturn(1);
        }
    }

}
