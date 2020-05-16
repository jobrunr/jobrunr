package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class MySQLGsonE2ETest extends AbstractE2EGsonTest {

    @Container
    private static MySQLContainer sqlContainer = new MySQLContainer<>();

    @Container
    private static MySqlGsonBackgroundJobContainer backgroundJobServer = new MySqlGsonBackgroundJobContainer(sqlContainer);

    @Override
    protected StorageProvider getStorageProviderForClient() {
        return backgroundJobServer.getStorageProviderForClient();
    }

    @Override
    protected AbstractBackgroundJobSqlContainer backgroundJobServer() {
        return backgroundJobServer;
    }
}
