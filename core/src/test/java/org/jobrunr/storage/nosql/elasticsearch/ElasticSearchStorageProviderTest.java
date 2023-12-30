package org.jobrunr.storage.nosql.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.ErrorCause;
import co.elastic.clients.elasticsearch._types.ErrorResponse;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
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

import static org.jobrunr.storage.StorageProviderUtils.DatabaseOptions.CREATE;
import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

@Disabled
@Testcontainers
class ElasticSearchStorageProviderTest extends StorageProviderTest {

    @Container
    private static final ElasticsearchContainer elasticSearchContainer = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.10.3")
            .withEnv("ES_JAVA_OPTS", "-Xmx2048m")
            .withEnv("xpack.security.enabled", Boolean.FALSE.toString())
            .withPassword("password")
            .withExposedPorts(9200);

    @Override
    protected void cleanup() {
        try {
            ElasticsearchClient elasticsearchClient = elasticSearchClient();
            elasticsearchClient.cluster().putSettings(b -> b.transient_("action.destructive_requires_name", JsonData.of(false)));
            elasticsearchClient.indices().delete(d -> d.index("_all"));
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
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials("elastic", "password")
        );

        final RestClient http = RestClient
                .builder(HttpHost.create(elasticSearchContainer.getHttpHostAddress()))
                .setHttpClientConfigCallback(cb -> cb.setDefaultCredentialsProvider(credentialsProvider))
                .build();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        final ElasticsearchTransport transport = new RestClientTransport(http, new JacksonJsonpMapper(objectMapper));
        return new ElasticsearchClient(transport);
    }

    public static class ThrowingElasticSearchStorageProvider extends ThrowingStorageProvider {

        public ThrowingElasticSearchStorageProvider(StorageProvider storageProvider) {
            super(storageProvider, "client");
        }

        @Override
        protected void makeStorageProviderThrowException(StorageProvider provider) throws Exception {
            ElasticsearchClient client = mock(ElasticsearchClient.class);
            ErrorResponse errorResponse = mock(ErrorResponse.class);
            lenient().when(errorResponse.error()).thenReturn(mock(ErrorCause.class));
            lenient().doThrow(new ElasticsearchException("Some index exception", errorResponse)).when(client).index(any(Function.class));
            lenient().doThrow(new ElasticsearchException("Some index exception", errorResponse)).when(client).bulk(any(BulkRequest.class));
            setInternalState(provider, "client", client);
        }
    }
}