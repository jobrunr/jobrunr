package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.elasticsearch.ElasticSearchStorageProvider;
import org.testcontainers.containers.Network;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

public class ElasticSearchJacksonBackgroundJobContainer extends AbstractBackgroundJobContainer {

    private final ElasticsearchContainer elasticSearch;
    private final Network network;

    public ElasticSearchJacksonBackgroundJobContainer(ElasticsearchContainer elasticSearch, Network network) {
        super("jobrunr-e2e-elasticsearch-jackson:1.0");
        this.elasticSearch = elasticSearch;
        this.network = network;
    }

    @Override
    public void start() {
        this
                .dependsOn(elasticSearch)
                .withNetwork(network)
                .withEnv("ELASTICSEARCH_HOST", "elasticsearch")
                .withEnv("ELASTICSEARCH_PORT", String.valueOf(9200));

        super.start();
    }

    @Override
    public StorageProvider getStorageProviderForClient() {
        return new ElasticSearchStorageProvider(elasticSearch.getContainerIpAddress(), elasticSearch.getFirstMappedPort());
    }

}
