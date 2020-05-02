package org.jobrunr.storage.sql.postgres;

import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;

class PostgresStorageProviderTest extends AbstractPostgresStorageProviderTest {

    private static PGSimpleDataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = new PGSimpleDataSource();
            dataSource.setURL(sqlContainer.getJdbcUrl());
            dataSource.setUser(sqlContainer.getUsername());
            dataSource.setPassword(sqlContainer.getPassword());
        }
        return dataSource;
    }
}