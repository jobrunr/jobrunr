package org.jobrunr.storage.sql.mysql;

import org.jobrunr.storage.sql.SqlStorageProviderTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.extension.AfterAllSubclasses;
import org.junit.jupiter.extension.BeforeAllSubclasses;
import org.junit.jupiter.extension.ForAllSubclassesExtension;
import org.testcontainers.containers.MySQLContainer;

@ExtendWith(ForAllSubclassesExtension.class)
public abstract class AbstractMySQLStorageProviderTest extends SqlStorageProviderTest {

    protected static MySQLContainer sqlContainer = new MySQLContainer<>().withEnv("TZ", "UTC");

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
