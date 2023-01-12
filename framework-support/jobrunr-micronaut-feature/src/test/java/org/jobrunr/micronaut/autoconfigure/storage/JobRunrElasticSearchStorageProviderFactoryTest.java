package org.jobrunr.micronaut.autoconfigure.storage;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.cluster.ElasticsearchClusterClient;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import io.micronaut.context.ApplicationContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.elasticsearch.ElasticSearchStorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.function.Function;

import static org.jobrunr.micronaut.MicronautAssertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest(rebuildContext = true)
class JobRunrElasticSearchStorageProviderFactoryTest {

    @Inject
    ApplicationContext context;

    @BeforeEach
    void setupElasticSearchClient() throws IOException {
        context.registerSingleton(elasticsearchClient());
    }

    @Test
    void elasticSearchStorageProviderAutoConfigurationTest() {
        assertThat(context).hasSingleBean(StorageProvider.class);
        assertThat(context.getBean(StorageProvider.class))
          .isInstanceOf(ElasticSearchStorageProvider.class)
          .hasJobMapper();
        assertThat(context).doesNotHaveBean(InMemoryStorageProvider.class);
    }

    public static ElasticsearchClient elasticsearchClient() throws IOException {
        final ElasticsearchClient client = mock(ElasticsearchClient.class);

        final GetResponse<?> getResponse = mock(GetResponse.class);
        when(getResponse.found()).thenReturn(true);
        when(client.get(any(Function.class), any())).thenAnswer(args -> getResponse);

        final BooleanResponse exists = new BooleanResponse(true);
        when(client.exists(any(Function.class))).thenReturn(exists);

        final ElasticsearchIndicesClient indices = mock(ElasticsearchIndicesClient.class);
        when(client.indices()).thenReturn(indices);

        when(indices.exists(any(Function.class))).thenReturn(exists);
        when(indices.exists(any(ExistsRequest.class))).thenReturn(exists);

        final ElasticsearchClusterClient cluster = mock(ElasticsearchClusterClient.class);
        when(client.cluster()).thenReturn(cluster);

        return client;
    }
}
