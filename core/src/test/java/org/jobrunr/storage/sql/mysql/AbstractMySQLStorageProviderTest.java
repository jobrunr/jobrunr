package org.jobrunr.storage.sql.mysql;

import org.jobrunr.storage.sql.SqlStorageProviderTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.extension.AfterAllSubclasses;
import org.junit.jupiter.extension.BeforeAllSubclasses;
import org.junit.jupiter.extension.ForAllSubclassesExtension;
import org.testcontainers.containers.MySQLContainer;

import java.time.Duration;
import java.time.Instant;

import static java.time.Instant.now;

@ExtendWith(ForAllSubclassesExtension.class)
public abstract class AbstractMySQLStorageProviderTest extends SqlStorageProviderTest {

    protected static MySQLContainer sqlContainer = new MySQLContainer<>("mysql:5.7").withEnv("TZ", "UTC");

    @BeforeAllSubclasses
    public static void startSqlContainer() {
        Instant before = now();
        sqlContainer.start();
        printSqlContainerDetails(sqlContainer, Duration.between(before, now()));
    }

    @AfterAllSubclasses
    public static void stopSqlContainer() {
        sqlContainer.stop();
    }
}
