package org.jobrunr.storage.sql.mysql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;

import javax.sql.DataSource;

class HikariMySQLStorageProviderTest extends AbstractMySQLStorageProviderTest {

    private static HikariDataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            HikariConfig config = new HikariConfig();

            config.setDriverClassName("com.mysql.jdbc.Driver");
            config.setJdbcUrl(sqlContainer.getJdbcUrl() + "?rewriteBatchedStatements=true&useSSL=false");
            config.setUsername(sqlContainer.getUsername());
            config.setPassword(sqlContainer.getPassword());
            dataSource = new HikariDataSource(config);
        }
        return dataSource;
    }

    @AfterAll
    public static void destroyDatasource() {
        dataSource.close();
    }
}