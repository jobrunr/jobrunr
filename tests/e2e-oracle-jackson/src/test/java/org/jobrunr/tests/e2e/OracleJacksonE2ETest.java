package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.executioncondition.RunTestBetween;
import org.junit.jupiter.executioncondition.RunTestIfDockerImageExists;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@RunTestIfDockerImageExists("container-registry.oracle.com/database/standard:12.1.0.2")
@RunTestBetween(from = "03:00", to = "07:00")
@Testcontainers
public class OracleJacksonE2ETest extends AbstractE2EJacksonSqlTest {

    @Container
    private static OracleContainer sqlContainer = new OracleContainer("container-registry.oracle.com/database/standard:12.1.0.2")
            .withStartupTimeoutSeconds(900)
            .withConnectTimeoutSeconds(500)
            .withEnv("DB_SID", "ORCL")
            .withEnv("DB_PASSWD", "oracle")
            .withSharedMemorySize(4294967296L);

    @Container
    private static OracleJacksonBackgroundJobContainer backgroundJobServer = new OracleJacksonBackgroundJobContainer(sqlContainer);

    @Override
    protected StorageProvider getStorageProviderForClient() {
        return backgroundJobServer.getStorageProviderForClient();
    }

    @Override
    protected AbstractBackgroundJobSqlContainer backgroundJobServer() {
        return backgroundJobServer;
    }
}
