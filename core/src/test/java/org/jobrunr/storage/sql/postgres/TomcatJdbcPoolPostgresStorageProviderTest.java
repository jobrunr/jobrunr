package org.jobrunr.storage.sql.postgres;

import javax.sql.DataSource;

class TomcatJdbcPoolPostgresStorageProviderTest extends AbstractPostgresStorageProviderTest {

    @Override
    protected DataSource getDataSource() {
        org.apache.tomcat.jdbc.pool.DataSource ds = new org.apache.tomcat.jdbc.pool.DataSource();
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setUrl(sqlContainer.getJdbcUrl());
        ds.setUsername(sqlContainer.getUsername());
        ds.setPassword(sqlContainer.getPassword());
        return ds;
    }
}