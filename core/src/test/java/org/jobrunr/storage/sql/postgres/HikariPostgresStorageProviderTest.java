package org.jobrunr.storage.sql.postgres;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

class HikariPostgresStorageProviderTest extends AbstractPostgresStorageProviderTest {

    @Override
    protected DataSource getDataSource() {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(sqlContainer.getJdbcUrl());
        config.setUsername(sqlContainer.getUsername());
        config.setPassword(sqlContainer.getPassword());
        return new HikariDataSource(config);
    }
}