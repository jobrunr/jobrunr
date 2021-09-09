package org.jobrunr.storage.nosql.elasticsearch;

import org.apache.http.HttpHost;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderTest;
import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.AfterAll;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;

import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

@Testcontainers
class ElasticSearchStorageProviderTest extends StorageProviderTest {

    @Container
    private static final ElasticsearchContainer elasticSearchContainer = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.10.1").withExposedPorts(9200);

    private static RestHighLevelClient restHighLevelClient;

    @Override
    protected void cleanup() {
        try {
            getElasticSearchClient().indices().delete(new DeleteIndexRequest("_all"), RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected StorageProvider getStorageProvider() {
        final ElasticSearchStorageProvider elasticSearchStorageProvider = new ElasticSearchStorageProvider(getElasticSearchClient(), DatabaseOptions.CREATE, rateLimit().withoutLimits());
        elasticSearchStorageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
        return elasticSearchStorageProvider;
    }

    @Override
    protected ThrowingStorageProvider makeThrowingStorageProvider(StorageProvider storageProvider) {
        return new ThrowingElasticSearchStorageProvider(storageProvider);
    }

    @AfterAll
    public static void closeElasticSearch() throws IOException {
        restHighLevelClient.close();
    }

    private static RestHighLevelClient getElasticSearchClient() {
        if (restHighLevelClient == null) {
            restHighLevelClient = new RestHighLevelClient(
                    RestClient.builder(
                            new HttpHost(elasticSearchContainer.getContainerIpAddress(), elasticSearchContainer.getMappedPort(9200), "http")));
        }
        return restHighLevelClient;
    }

    public static class ThrowingElasticSearchStorageProvider extends ThrowingStorageProvider {

        public ThrowingElasticSearchStorageProvider(StorageProvider storageProvider) {
            super(storageProvider, "client");
        }

        @Override
        protected void makeStorageProviderThrowException(StorageProvider storageProvider) throws Exception {
            RestHighLevelClient clientMock = mock(RestHighLevelClient.class);
            when(clientMock.index(any(), any())).thenThrow(new ElasticsearchException("Some index exception"));
            when(clientMock.bulk(any(), any())).thenThrow(new ElasticsearchException("Some bulk index exception"));
            setInternalState(storageProvider, "client", clientMock);
        }
    }
}