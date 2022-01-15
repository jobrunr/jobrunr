package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public class SQLServerGsonE2ETest extends AbstractE2EGsonTest {

    @Container
    private static final MSSQLServerContainer sqlContainer = new MSSQLServerContainer<>(DockerImageName.parse("mcr.microsoft.com/azure-sql-edge").asCompatibleSubstituteFor("mcr.microsoft.com/mssql/server"));

    @Container
    private static final SQLServerGsonBackgroundJobContainer backgroundJobServer = new SQLServerGsonBackgroundJobContainer(sqlContainer);

    @Override
    protected StorageProvider getStorageProviderForClient() {
        return backgroundJobServer.getStorageProviderForClient();
    }

    @Override
    protected AbstractBackgroundJobSqlContainer backgroundJobServer() {
        return backgroundJobServer;
    }
}
