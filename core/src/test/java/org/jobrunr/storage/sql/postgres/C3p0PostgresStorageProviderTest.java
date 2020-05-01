package org.jobrunr.storage.sql.postgres;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import javax.sql.DataSource;

class C3p0PostgresStorageProviderTest extends AbstractPostgresStorageProviderTest {

    @Override
    protected DataSource getDataSource() {
        ComboPooledDataSource ds = new ComboPooledDataSource();

        ds.setJdbcUrl(sqlContainer.getJdbcUrl());
        ds.setUser(sqlContainer.getUsername());
        ds.setPassword(sqlContainer.getPassword());
        return ds;
    }
}