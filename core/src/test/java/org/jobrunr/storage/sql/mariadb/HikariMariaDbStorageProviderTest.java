package org.jobrunr.storage.sql.mariadb;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

class HikariMariaDbStorageProviderTest extends AbstractMariaDbStorageProviderTest {

    private static HikariDataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            HikariConfig config = new HikariConfig();

            config.setJdbcUrl(sqlContainer.getJdbcUrl() + "?rewriteBatchedStatements=true");
            config.setUsername(sqlContainer.getUsername());
            config.setPassword(sqlContainer.getPassword());
            dataSource = new HikariDataSource(config);
        }
        return dataSource;
    }
}