package org.jobrunr.micronaut.autoconfigure.storage;

import io.micronaut.context.ApplicationContext;
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
import org.jobrunr.storage.nosql.elasticsearch.ElasticSearchStorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.jobrunr.micronaut.MicronautAssertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest(rebuildContext = true)
class ElasticSearchStorageProviderJobRunrFactoryTest {

    @Inject
    ApplicationContext context;

    @BeforeEach
    void setupElasticSearchClient() throws IOException {
        context.registerSingleton(elasticSearchRestHighLevelClient());
    }

    @Test
    void elasticSearchStorageProviderAutoConfigurationTest() {
        assertThat(context).hasSingleBean(StorageProvider.class);
        assertThat(context.getBean(StorageProvider.class))
                .isInstanceOf(ElasticSearchStorageProvider.class)
                .hasJobMapper();
        assertThat(context).doesNotHaveBean(InMemoryStorageProvider.class);
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
