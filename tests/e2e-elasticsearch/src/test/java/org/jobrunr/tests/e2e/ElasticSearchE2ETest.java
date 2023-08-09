package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.Disabled;
import org.testcontainers.containers.Network;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@Disabled("My NAS only has 8GB of RAM which is not enough for Elastic and thus this test is flaky")
public class ElasticSearchE2ETest extends AbstractE2ETest {

    private static final Network network = Network.newNetwork();

    @Container
    private static final ElasticsearchContainer elasticSearchContainer = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.10.1")
            .withNetwork(network)
            .withNetworkAliases("elasticsearch")
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
