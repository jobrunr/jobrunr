package org.jobrunr.storage.sql.mariadb;

import org.jobrunr.storage.sql.SqlStorageProviderTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.extension.AfterAllSubclasses;
import org.junit.jupiter.extension.BeforeAllSubclasses;
import org.junit.jupiter.extension.ForAllSubclassesExtension;
import org.testcontainers.containers.MariaDBContainer;

@ExtendWith(ForAllSubclassesExtension.class)
public abstract class AbstractMariaDbStorageProviderTest extends SqlStorageProviderTest {

    protected static MariaDBContainer sqlContainer = (MariaDBContainer) new MariaDBContainer().withEnv("TZ", "UTC");

    @BeforeAllSubclasses
    public static void startSqlContainer() {
        sqlContainer.start();
        System.out.println("=========================================================");
        System.out.println(sqlContainer.getJdbcUrl());
        System.out.println("=========================================================");
    }

    @AfterAllSubclasses
    public static void stopSqlContainer() {
        sqlContainer.stop();
    }
}
