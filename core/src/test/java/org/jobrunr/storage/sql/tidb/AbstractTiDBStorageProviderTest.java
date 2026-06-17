package org.jobrunr.storage.sql.tidb;

import com.zaxxer.hikari.HikariDataSource;
import org.jobrunr.storage.sql.SqlStorageProviderTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.extension.AfterAllSubclasses;
import org.junit.jupiter.extension.BeforeAllSubclasses;
import org.junit.jupiter.extension.ForAllSubclassesExtension;
import org.testcontainers.tidb.TiDBContainer;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.Instant;

import static java.time.Instant.now;
import static org.jobrunr.storage.sql.SqlTestUtils.toHikariDataSource;

@ExtendWith(ForAllSubclassesExtension.class)
public abstract class AbstractTiDBStorageProviderTest extends SqlStorageProviderTest {

    protected static TiDBContainer tidbContainer = new TiDBContainer("pingcap/tidb:v8.5.5").withEnv("TZ", "UTC");

    protected static HikariDataSource dataSource;

    @Override
    public DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = toHikariDataSource(tidbContainer, "?rewriteBatchedStatements=true&useSSL=false");
        }
        return dataSource;
    }

    @AfterAll
    public static void destroyDatasource() {
        dataSource.close();
        dataSource = null;
    }

    @BeforeAllSubclasses
    public static void startSqlContainer() {
        Instant before = now();
        tidbContainer.start();
        printSqlContainerDetails(tidbContainer, Duration.between(before, now()));
    }

    @AfterAllSubclasses
    public static void stopSqlContainer() {
        tidbContainer.stop();
    }
}
