package org.jobrunr.storage.sql.postgres;

import org.apache.commons.dbcp2.BasicDataSource;

import javax.sql.DataSource;

class CommonsDbcpPostgresStorageProviderTest extends AbstractPostgresStorageProviderTest {

    @Override
    protected DataSource getDataSource() {
        BasicDataSource ds = new BasicDataSource();
        ds.setUrl(sqlContainer.getJdbcUrl());
        ds.setUsername(sqlContainer.getUsername());
        ds.setPassword(sqlContainer.getPassword());
        return ds;
    }
}