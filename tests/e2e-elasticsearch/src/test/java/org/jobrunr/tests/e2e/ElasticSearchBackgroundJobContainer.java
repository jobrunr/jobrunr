package org.jobrunr.tests.e2e;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.elasticsearch.ElasticSearchStorageProvider;
import org.testcontainers.containers.Network;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

public class ElasticSearchBackgroundJobContainer extends AbstractBackgroundJobContainer {

    private final ElasticsearchContainer elasticSearchContainer;

    public ElasticSearchBackgroundJobContainer(ElasticsearchContainer elasticSearch, Network network) {
        super("jobrunr-e2e-elasticsearch:1.0");
        this.elasticSearchContainer = elasticSearch;
        this.withNetwork(network);
    }

    @Override
    public void start() {
        this
                .dependsOn(elasticSearchContainer)
                .withEnv("ELASTICSEARCH_HOST", elasticSearchContainer.getNetworkAliases().get(0))
                .withEnv("ELASTICSEARCH_PORT", String.valueOf(9200));

        super.start();
    }

    @Override
    public StorageProvider getStorageProviderForClient() {
        HttpHost httpHost = new HttpHost(elasticSearchContainer.getHost(), elasticSearchContainer.getFirstMappedPort(), "http");
        RestHighLevelClient restHighLevelClient = new RestHighLevelClient(RestClient.builder(httpHost).setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder.setSocketTimeout(100 * 1000)));
        return new ElasticSearchStorageProvider(restHighLevelClient);
    }

}
