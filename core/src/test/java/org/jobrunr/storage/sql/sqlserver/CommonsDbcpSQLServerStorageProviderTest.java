package org.jobrunr.storage.sql.sqlserver;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.AfterAll;

import javax.sql.DataSource;
import java.sql.SQLException;

class CommonsDbcpSQLServerStorageProviderTest extends AbstractSQLServerStorageProviderTest {

    private static BasicDataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = new BasicDataSource();
            dataSource.setUrl(sqlContainer.getJdbcUrl());
            dataSource.setUsername(sqlContainer.getUsername());
            dataSource.setPassword(sqlContainer.getPassword());
        }
        return dataSource;
    }

    @AfterAll
    public static void destroyDatasource() throws SQLException {
        dataSource.close();
    }
}