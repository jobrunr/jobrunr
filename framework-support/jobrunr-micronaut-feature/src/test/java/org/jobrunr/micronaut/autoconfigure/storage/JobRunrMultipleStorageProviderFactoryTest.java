package org.jobrunr.micronaut.autoconfigure.storage;


import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.ClusterClient;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.SqlStorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;

import static org.jobrunr.micronaut.MicronautAssertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest(rebuildContext = true)
class JobRunrMultipleStorageProviderFactoryTest {

    @Inject
    ApplicationContext context;

    @BeforeEach
    void setupDataSource() throws SQLException, IOException {
        context.registerSingleton(elasticSearchRestHighLevelClient());
        context.registerSingleton(dataSource());
    }

    @Test
    @Property(name = "jobrunr.database.skip-create", value = "true")
    @Property(name = "jobrunr.database.type", value = "sql")
    void sqlStorageProviderAutoConfigurationTest() {
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

    public RestHighLevelClient elasticSearchRestHighLevelClient() throws IOException {
        RestHighLevelClient restHighLevelClientMock = mock(RestHighLevelClient.class);
        IndicesClient indicesClientMock = mock(IndicesClient.class);
        ClusterClient clusterClientMock = mock(ClusterClient.class);
        when(restHighLevelClientMock.indices()).thenReturn(indicesClientMock);
        when(restHighLevelClientMock.cluster()).thenReturn(clusterClientMock);
        when(indicesClientMock.exists(any(GetIndexRequest.class), eq(RequestOptions.DEFAULT))).thenReturn(true);

        GetResponse getResponse = mock(GetResponse.class);
        when(getResponse.isExists()).thenReturn(true);
        when(restHighLevelClientMock.get(any(GetRequest.class), eq(RequestOptions.DEFAULT))).thenReturn(getResponse);
        return restHighLevelClientMock;
    }

}
