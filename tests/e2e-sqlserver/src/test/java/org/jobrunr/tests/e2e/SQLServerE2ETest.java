package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public class SQLServerE2ETest extends AbstractE2ETest {

    private static final Network network = Network.newNetwork();

    @Container
    private final MSSQLServerContainer sqlContainer = new MSSQLServerContainer<>(DockerImageName.parse("mcr.microsoft.com/azure-sql-edge").asCompatibleSubstituteFor("mcr.microsoft.com/mssql/server"))
            .withNetwork(network)
            .withNetworkAliases("sqlserver");

    @Container
    private final SQLServerBackgroundJobContainer backgroundJobServer = new SQLServerBackgroundJobContainer(sqlContainer, network);

    @Override
    protected StorageProvider getStorageProviderForClient() {
        return backgroundJobServer.getStorageProviderForClient();
    }

    @Override
    protected AbstractBackgroundJobSqlContainer backgroundJobServer() {
        return backgroundJobServer;
    }
}
