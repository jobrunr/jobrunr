package org.jobrunr.storage.sql.sqlite;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jobrunr.storage.sql.SqlStorageProviderTest;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;

class HikariSqLiteStorageProviderTest extends SqlStorageProviderTest {

    @Override
    protected DataSource getDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:/tmp/jobrunr-test.db");
        return new HikariDataSource(config);
    }
}