package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class PostgresJacksonE2ETest extends AbstractE2EJacksonSqlTest {

    @Container
    private static PostgreSQLContainer sqlContainer = new PostgreSQLContainer<>();

    @Container
    private static PostgresJacksonBackgroundJobContainer backgroundJobServer = new PostgresJacksonBackgroundJobContainer(sqlContainer);

    @Override
    protected StorageProvider getStorageProviderForClient() {
        return backgroundJobServer.getStorageProviderForClient();
    }

    @Override
    protected AbstractBackgroundJobSqlContainer backgroundJobServer() {
        return backgroundJobServer;
    }
}
