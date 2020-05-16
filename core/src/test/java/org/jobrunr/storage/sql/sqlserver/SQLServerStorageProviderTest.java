package org.jobrunr.storage.sql.sqlserver;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;

import javax.sql.DataSource;

class SQLServerStorageProviderTest extends AbstractSQLServerStorageProviderTest {

    private static SQLServerDataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = new SQLServerDataSource();
            dataSource.setURL(sqlContainer.getJdbcUrl());
            dataSource.setUser(sqlContainer.getUsername());
            dataSource.setPassword(sqlContainer.getPassword());
        }
        return dataSource;
    }
}