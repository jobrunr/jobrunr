package org.jobrunr.storage.sql.mariadb;

import org.apache.commons.dbcp2.BasicDataSource;

import javax.sql.DataSource;

class CommonsDbcpMariaDbStorageProviderTest extends AbstractMariaDbStorageProviderTest {

    private static BasicDataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = new BasicDataSource();
            dataSource.setUrl(sqlContainer.getJdbcUrl() + "?rewriteBatchedStatements=true");
            dataSource.setUsername(sqlContainer.getUsername());
            dataSource.setPassword(sqlContainer.getPassword());
        }
        return dataSource;
    }
}