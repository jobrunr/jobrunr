package org.jobrunr.storage.sql.mariadb;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

class HikariMariaDbStorageProviderTest extends AbstractMariaDbStorageProviderTest {

    @Override
    protected DataSource getDataSource() {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(sqlContainer.getJdbcUrl() + "?rewriteBatchedStatements=true");
        config.setUsername(sqlContainer.getUsername());
        config.setPassword(sqlContainer.getPassword());
        return new HikariDataSource(config);
    }
}