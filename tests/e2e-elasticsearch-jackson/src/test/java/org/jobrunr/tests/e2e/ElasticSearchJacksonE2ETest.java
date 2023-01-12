package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.testcontainers.containers.Network;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static java.util.Map.of;

@Testcontainers
public class ElasticSearchJacksonE2ETest extends AbstractE2EJacksonTest {

    private static final Network network = Network.newNetwork();

    @Container
    private static final ElasticsearchContainer elasticSearchContainer = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.17.8")
      .withNetwork(network)
      .withNetworkAliases("elasticsearch")
      .withEnv(of(
        "network.host", "0.0.0.0",
        "xpack.security.enabled", "false",
        "ES_JAVA_OPTS", "-Xmx512m"
        )).withExposedPorts(9200);

    @Container
    private static final ElasticSearchJacksonBackgroundJobContainer backgroundJobServer = new ElasticSearchJacksonBackgroundJobContainer(elasticSearchContainer, network);

    @Override
    protected StorageProvider getStorageProviderForClient() {
        return backgroundJobServer.getStorageProviderForClient();
    }

    @Override
    protected AbstractBackgroundJobContainer backgroundJobServer() {
        return backgroundJobServer;
    }
}
