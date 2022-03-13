package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class MySqlJacksonE2ETest extends AbstractE2EJacksonTest {

    @Container
    private static final MySQLContainer sqlContainer = new MySQLContainer<>();

    @Container
    private static final MySqlJacksonBackgroundJobContainer backgroundJobServer = new MySqlJacksonBackgroundJobContainer(sqlContainer);

    @Override
    protected StorageProvider getStorageProviderForClient() {
        return backgroundJobServer.getStorageProviderForClient();
    }

    @Override
    protected AbstractBackgroundJobSqlContainer backgroundJobServer() {
        return backgroundJobServer;
    }
}
