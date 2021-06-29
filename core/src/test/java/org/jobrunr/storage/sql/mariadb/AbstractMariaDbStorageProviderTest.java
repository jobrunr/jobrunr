package org.jobrunr.storage.sql.mariadb;

import org.jobrunr.storage.sql.SqlStorageProviderTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.extension.AfterAllSubclasses;
import org.junit.jupiter.extension.BeforeAllSubclasses;
import org.junit.jupiter.extension.ForAllSubclassesExtension;
import org.testcontainers.containers.MariaDBContainer;

import java.time.Duration;
import java.time.Instant;

import static java.time.Instant.now;

@ExtendWith(ForAllSubclassesExtension.class)
public abstract class AbstractMariaDbStorageProviderTest extends SqlStorageProviderTest {

    protected static MariaDBContainer sqlContainer = new MariaDBContainer<>("mariadb").withEnv("TZ", "UTC");

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
