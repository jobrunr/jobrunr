package org.jobrunr.storage.sql.oracle;

import org.jobrunr.storage.sql.DatabaseCleaner;
import org.jobrunr.storage.sql.SqlStorageProviderTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.extension.AfterAllSubclasses;
import org.junit.jupiter.extension.BeforeAllSubclasses;
import org.junit.jupiter.extension.ForAllSubclassesExtension;
import org.testcontainers.containers.OracleContainer;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.Instant;

import static java.time.Instant.now;

@ExtendWith(ForAllSubclassesExtension.class)
public abstract class AbstractOracleStorageProviderTest extends SqlStorageProviderTest {

    protected static OracleContainer sqlContainer = new OracleContainer("gvenzl/oracle-xe")
            .withStartupTimeoutSeconds(900)
            .withConnectTimeoutSeconds(500)
            .withEnv("DB_SID", "ORCL")
            .withEnv("DB_PASSWD", "oracle")
            .withSharedMemorySize(4294967296L);

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

    @Override
    protected DatabaseCleaner getDatabaseCleaner(DataSource dataSource) {
        return new DatabaseCleaner(dataSource, this::canIgnoreException);
    }

    private boolean canIgnoreException(Exception e) {
        return e.getMessage().contains("ORA-00942");
    }
}
