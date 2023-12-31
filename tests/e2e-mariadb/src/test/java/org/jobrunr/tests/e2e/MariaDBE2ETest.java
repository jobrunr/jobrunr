package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class MariaDBE2ETest extends AbstractE2ETest {

    private static final Network network = Network.newNetwork();

    @Container
    private final MariaDBContainer sqlContainer = new MariaDBContainer<>("mariadb:10.6")
            .withNetwork(network)
            .withNetworkAliases("mariadb");

    @Container
    private final MariaDBBackgroundJobContainer backgroundJobServer = new MariaDBBackgroundJobContainer(sqlContainer, network);

    @Override
    protected StorageProvider getStorageProviderForClient() {
        return backgroundJobServer.getStorageProviderForClient();
    }

    @Override
    protected AbstractBackgroundJobSqlContainer backgroundJobServer() {
        return backgroundJobServer;
    }
}
