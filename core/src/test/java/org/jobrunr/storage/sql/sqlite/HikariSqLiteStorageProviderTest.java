package org.jobrunr.storage.sql.sqlite;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jobrunr.storage.sql.SqlStorageProviderTest;
import org.junit.jupiter.api.AfterAll;

import javax.sql.DataSource;

class HikariSqLiteStorageProviderTest extends SqlStorageProviderTest {

    private static HikariDataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:sqlite:/tmp/jobrunr-test.db");
            dataSource = new HikariDataSource(config);
        }
        return dataSource;
    }

    @AfterAll
    public static void destroyDatasource() {
        dataSource.close();
    }
}