package org.jobrunr.storage.sql.mariadb;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import javax.sql.DataSource;

class C3p0MariaDbStorageProviderTest extends AbstractMariaDbStorageProviderTest {

    @Override
    protected DataSource getDataSource() {
        ComboPooledDataSource ds = new ComboPooledDataSource();

        ds.setJdbcUrl(sqlContainer.getJdbcUrl() + "?rewriteBatchedStatements=true");
        ds.setUser(sqlContainer.getUsername());
        ds.setPassword(sqlContainer.getPassword());
        return ds;
    }
}