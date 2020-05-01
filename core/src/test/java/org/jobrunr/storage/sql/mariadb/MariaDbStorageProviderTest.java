package org.jobrunr.storage.sql.mariadb;

import org.mariadb.jdbc.MariaDbPoolDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;

class MariaDbStorageProviderTest extends AbstractMariaDbStorageProviderTest {

    @Override
    protected DataSource getDataSource() {
        try {
            MariaDbPoolDataSource dataSource = new MariaDbPoolDataSource();
            dataSource.setUrl(sqlContainer.getJdbcUrl() + "?rewriteBatchedStatements=true&pool=true");
            dataSource.setUser(sqlContainer.getUsername());
            dataSource.setPassword(sqlContainer.getPassword());
            return dataSource;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}