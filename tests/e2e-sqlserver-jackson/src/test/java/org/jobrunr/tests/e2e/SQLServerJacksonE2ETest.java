package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class SQLServerJacksonE2ETest extends AbstractE2EJacksonTest {

    @Container
    private static final MSSQLServerContainer sqlContainer = new MSSQLServerContainer<>();

    @Container
    private static final SQLServerJacksonBackgroundJobContainer backgroundJobServer = new SQLServerJacksonBackgroundJobContainer(sqlContainer);

    @Override
    protected StorageProvider getStorageProviderForClient() {
        return backgroundJobServer.getStorageProviderForClient();
    }

    @Override
    protected AbstractBackgroundJobSqlContainer backgroundJobServer() {
        return backgroundJobServer;
    }
}
