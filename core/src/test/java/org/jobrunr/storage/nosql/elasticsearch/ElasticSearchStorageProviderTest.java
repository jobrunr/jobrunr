package org.jobrunr.storage.nosql.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderTest;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.Disabled;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.function.Function;

import static java.util.Map.of;
import static org.jobrunr.storage.StorageProviderUtils.DatabaseOptions.CREATE;
import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

@Disabled
@Testcontainers
class ElasticSearchStorageProviderTest extends StorageProviderTest {

    @Container
    private static final ElasticsearchContainer elasticSearchContainer = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.17.8")
      .withEnv(of("ES_JAVA_OPTS", "-Xmx512m"))
      .withExposedPorts(9200);

    private static ElasticsearchClient client;

    @Override
    protected void cleanup() {
        try {
            elasticSearchClient().indices().delete(d -> d.index("_all"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected StorageProvider getStorageProvider() {
        final ElasticSearchStorageProvider provider = new ElasticSearchStorageProvider(elasticSearchClient(), CREATE, rateLimit().withoutLimits());
        provider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
        return provider;
    }

    @Override
    protected ThrowingStorageProvider makeThrowingStorageProvider(StorageProvider storageProvider) {
        return new ThrowingElasticSearchStorageProvider(storageProvider);
    }

    private ElasticsearchClient elasticSearchClient() {
        final RestClient http = RestClient.builder(
            new HttpHost(elasticSearchContainer.getHost(), elasticSearchContainer.getMappedPort(9200), "http"))
          .build();
        final ElasticsearchTransport transport = new RestClientTransport(http, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }

    public static class ThrowingElasticSearchStorageProvider extends ThrowingStorageProvider {

        public ThrowingElasticSearchStorageProvider(StorageProvider storageProvider) {
            super(storageProvider, "client");
        }

        @Override
        protected void makeStorageProviderThrowException(StorageProvider provider) throws Exception {
            ElasticsearchClient client = mock(ElasticsearchClient.class);
            when(client.index(any(Function.class))).thenThrow(new ElasticsearchException("Some index exception", null));
            when(client.bulk(any(Function.class))).thenThrow(new ElasticsearchException("Some bulk index exception", null));
            setInternalState(provider, "client", client);
        }
    }
}