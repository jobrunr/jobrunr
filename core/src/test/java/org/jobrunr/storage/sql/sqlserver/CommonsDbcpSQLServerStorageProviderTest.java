package org.jobrunr.storage.sql.sqlserver;

import org.apache.commons.dbcp2.BasicDataSource;

import javax.sql.DataSource;

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
}