package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.testcontainers.containers.Network;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class ElasticSearchE2ETest extends AbstractE2ETest {

    private static final Network network = Network.newNetwork();

    @Container
    private static final ElasticsearchContainer elasticSearchContainer = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.10.3")
            .withNetwork(network)
            .withNetworkAliases("elasticsearch")
            .withEnv("ES_JAVA_OPTS", "-Xmx2048m")
            .withEnv("xpack.security.enabled", Boolean.FALSE.toString())
            .withPassword("password")
            .withExposedPorts(9200);

    @Container
    private static final ElasticSearchBackgroundJobContainer backgroundJobServer = new ElasticSearchBackgroundJobContainer(elasticSearchContainer, network);

    @Override
    protected StorageProvider getStorageProviderForClient() {
        return backgroundJobServer.getStorageProviderForClient();
    }

    @Override
    protected AbstractBackgroundJobContainer backgroundJobServer() {
        return backgroundJobServer;
    }
}
