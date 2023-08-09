package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class PostgresE2ETest extends AbstractE2ETest {

    private static final Network network = Network.newNetwork();

    @Container
    private static final PostgreSQLContainer sqlContainer = new PostgreSQLContainer<>()
            .withNetwork(network)
            .withNetworkAliases("postgres");

    @Container
    private static final PostgresBackgroundJobContainer backgroundJobServer = new PostgresBackgroundJobContainer(sqlContainer, network);

    @Override
    protected StorageProvider getStorageProviderForClient() {
        return backgroundJobServer.getStorageProviderForClient();
    }

    @Override
    protected AbstractBackgroundJobSqlContainer backgroundJobServer() {
        return backgroundJobServer;
    }
}
