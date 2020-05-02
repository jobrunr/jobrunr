package org.jobrunr.storage.sql.mariadb;

import org.jobrunr.storage.sql.SqlStorageProviderTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.extension.AfterAllSubclasses;
import org.junit.jupiter.extension.BeforeAllSubclasses;
import org.junit.jupiter.extension.ForAllSubclassesExtension;
import org.testcontainers.containers.MariaDBContainer;

import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(ForAllSubclassesExtension.class)
public abstract class AbstractMariaDbStorageProviderTest extends SqlStorageProviderTest {

    private static AtomicInteger testSubClassCounter = new AtomicInteger(5);
    protected static MariaDBContainer sqlContainer = (MariaDBContainer) new MariaDBContainer().withEnv("TZ", "UTC");

    @BeforeAllSubclasses
    public static void startSqlContainer() {
        sqlContainer.start();
    }

    @AfterAllSubclasses
    public static void stopSqlContainer() {
        sqlContainer.stop();
    }
}
