package org.jobrunr.tests.e2e;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.SimpleJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.elasticsearch.ElasticSearchStorageProvider;
import org.testcontainers.containers.Network;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

public class ElasticSearchJacksonBackgroundJobContainer extends AbstractBackgroundJobContainer {

    private final ElasticsearchContainer elasticSearchContainer;
    private final Network network;

    public ElasticSearchJacksonBackgroundJobContainer(ElasticsearchContainer elasticSearch, Network network) {
        super("jobrunr-e2e-elasticsearch-jackson:1.0");
        this.elasticSearchContainer = elasticSearch;
        this.network = network;
    }

    @Override
    public void start() {
        this
                .dependsOn(elasticSearchContainer)
                .withNetwork(network)
                .withEnv("ELASTICSEARCH_HOST", "elasticsearch")
                .withEnv("ELASTICSEARCH_PORT", String.valueOf(9200));

        super.start();
    }

    @Override
    public StorageProvider getStorageProviderForClient() {
        final HttpHost httpHost = new HttpHost(elasticSearchContainer.getHost(), elasticSearchContainer.getFirstMappedPort(), "http");
        final RestClient restClient = RestClient
          .builder(httpHost)
          .setRequestConfigCallback(b -> b.setSocketTimeout(100 * 1000))
          .build();

        final RestClientTransport transport = new RestClientTransport(restClient, new SimpleJsonpMapper());
        final ElasticsearchClient restHighLevelClient = new ElasticsearchClient(transport);

        return new ElasticSearchStorageProvider(restHighLevelClient);
    }

}
