package org.jobrunr.storage.sql.sqlserver;

import org.jobrunr.storage.sql.SqlStorageProviderTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.extension.AfterAllSubclasses;
import org.junit.jupiter.extension.BeforeAllSubclasses;
import org.junit.jupiter.extension.ForAllSubclassesExtension;
import org.testcontainers.containers.MSSQLServerContainer;

@ExtendWith(ForAllSubclassesExtension.class)
public abstract class AbstractSQLServerStorageProviderTest extends SqlStorageProviderTest {

    protected static MSSQLServerContainer sqlContainer = new MSSQLServerContainer<>();

    @BeforeAllSubclasses
    public static void startSqlContainer() {
        sqlContainer.start();
    }

    @AfterAllSubclasses
    public static void stopSqlContainer() {
        sqlContainer.stop();
    }
}
