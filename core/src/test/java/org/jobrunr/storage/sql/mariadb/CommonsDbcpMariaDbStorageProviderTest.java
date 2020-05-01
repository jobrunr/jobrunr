package org.jobrunr.storage.sql.mariadb;

import org.apache.commons.dbcp2.BasicDataSource;

import javax.sql.DataSource;

class CommonsDbcpMariaDbStorageProviderTest extends AbstractMariaDbStorageProviderTest {

    @Override
    protected DataSource getDataSource() {
        BasicDataSource ds = new BasicDataSource();
        ds.setUrl(sqlContainer.getJdbcUrl() + "?rewriteBatchedStatements=true");
        ds.setUsername(sqlContainer.getUsername());
        ds.setPassword(sqlContainer.getPassword());
        return ds;
    }
}