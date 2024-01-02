package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class MySqlE2ETest extends AbstractE2ETest {

    private static final Network network = Network.newNetwork();

    @Container
    private final MySQLContainer sqlContainer = new MySQLContainer<>()
            .withNetwork(network)
            .withNetworkAliases("mysql");

    @Container
    private final MySqlBackgroundJobContainer backgroundJobServer = new MySqlBackgroundJobContainer(sqlContainer, network);

    @Override
    protected StorageProvider getStorageProviderForClient() {
        return backgroundJobServer.getStorageProviderForClient();
    }

    @Override
    protected AbstractBackgroundJobSqlContainer backgroundJobServer() {
        return backgroundJobServer;
    }
}
