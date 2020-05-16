package org.jobrunr.storage.sql.sqlserver;

import org.apache.tomcat.jdbc.pool.DataSource;

class TomcatJdbcPoolSQLServerStorageProviderTest extends AbstractSQLServerStorageProviderTest {

    private static DataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = new DataSource();
            dataSource.setUrl(sqlContainer.getJdbcUrl());
            dataSource.setUsername(sqlContainer.getUsername());
            dataSource.setPassword(sqlContainer.getPassword());
        }
        return dataSource;
    }
}