package org.jobrunr.storage.sql.mariadb;

import javax.sql.DataSource;

class TomcatJdbcPoolMariaDbStorageProviderTest extends AbstractMariaDbStorageProviderTest {

    @Override
    protected DataSource getDataSource() {
        org.apache.tomcat.jdbc.pool.DataSource ds = new org.apache.tomcat.jdbc.pool.DataSource();
        ds.setUrl(sqlContainer.getJdbcUrl() + "?rewriteBatchedStatements=true");
        ds.setUsername(sqlContainer.getUsername());
        ds.setPassword(sqlContainer.getPassword());
        return ds;
    }
}