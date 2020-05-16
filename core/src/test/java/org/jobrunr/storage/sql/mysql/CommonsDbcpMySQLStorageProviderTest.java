package org.jobrunr.storage.sql.mysql;

import org.apache.commons.dbcp2.BasicDataSource;

import javax.sql.DataSource;

class CommonsDbcpMySQLStorageProviderTest extends AbstractMySQLStorageProviderTest {

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