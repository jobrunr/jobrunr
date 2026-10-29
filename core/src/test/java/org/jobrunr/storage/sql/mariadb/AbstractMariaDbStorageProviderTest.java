package org.jobrunr.storage.sql.mariadb;

import com.zaxxer.hikari.HikariDataSource;
import org.jobrunr.storage.sql.SqlStorageProviderTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.extension.AfterAllSubclasses;
import org.junit.jupiter.extension.BeforeAllSubclasses;
import org.junit.jupiter.extension.ForAllSubclassesExtension;
import org.testcontainers.containers.MariaDBContainer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;

import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.storage.sql.SqlTestUtils.toHikariDataSource;

@ExtendWith(ForAllSubclassesExtension.class)
public abstract class AbstractMariaDbStorageProviderTest extends SqlStorageProviderTest {

    protected static MariaDBContainer sqlContainer = new MariaDBContainer<>("mariadb").withEnv("TZ", "UTC");

    protected static HikariDataSource dataSource;

    @Override
    public DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = toHikariDataSource(sqlContainer, "?rewriteBatchedStatements=true&useBulkStmts=false");
        }
        return dataSource;
    }

    @Test
    void jobStatsViewIsCreatedUsingSqlSecurityInvoker() throws SQLException {
        try (Connection connection = getDataSource().getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("select security_type from information_schema.views where table_schema = database() and table_name like '%jobrunr_jobs_stats'")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString("security_type")).isEqualTo("INVOKER");
        }
    }

    @AfterAll
    public static void destroyDatasource() {
        dataSource.close();
        dataSource = null;
    }

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
