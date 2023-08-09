package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

//@RunTestBetween(from = "00:00", to = "03:00")
@Testcontainers
public class OracleE2ETest extends AbstractE2ETest {

    private static final Network network = Network.newNetwork();

    @Container
    private static final OracleContainer sqlContainer = new OracleContainer("gvenzl/oracle-xe")
            .withStartupTimeoutSeconds(900)
            .withConnectTimeoutSeconds(500)
            .withEnv("DB_SID", "ORCL")
            .withEnv("DB_PASSWD", "oracle")
            .withSharedMemorySize(4294967296L)
            .withNetwork(network)
            .withNetworkAliases("oracledb");

    @Container
    private static final OracleBackgroundJobContainer backgroundJobServer = new OracleBackgroundJobContainer(sqlContainer, network);

    @Override
    protected StorageProvider getStorageProviderForClient() {
        return backgroundJobServer.getStorageProviderForClient();
    }

    @Override
    protected AbstractBackgroundJobSqlContainer backgroundJobServer() {
        return backgroundJobServer;
    }
}
