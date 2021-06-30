package org.jobrunr.storage.nosql.elasticsearch;

import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderTest;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.AfterAll;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;

import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;

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
        final ElasticSearchStorageProvider elasticSearchStorageProvider = new ElasticSearchStorageProvider(getElasticSearchClient(), rateLimit().withoutLimits());
        elasticSearchStorageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
        return elasticSearchStorageProvider;
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
}