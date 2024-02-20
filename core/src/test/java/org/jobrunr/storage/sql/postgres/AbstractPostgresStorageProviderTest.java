package org.jobrunr.storage.sql.postgres;

import org.jobrunr.storage.sql.SqlStorageProviderTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.extension.AfterAllSubclasses;
import org.junit.jupiter.extension.BeforeAllSubclasses;
import org.junit.jupiter.extension.ForAllSubclassesExtension;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Duration;
import java.time.Instant;

import static java.time.Instant.now;

@ExtendWith(ForAllSubclassesExtension.class)
public abstract class AbstractPostgresStorageProviderTest extends SqlStorageProviderTest {

    protected static PostgreSQLContainer sqlContainer = new PostgreSQLContainer<>("postgres:12");

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
