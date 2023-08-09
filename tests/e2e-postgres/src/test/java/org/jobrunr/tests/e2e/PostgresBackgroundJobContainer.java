package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.Network;

public class PostgresBackgroundJobContainer extends AbstractBackgroundJobSqlContainer {

    public PostgresBackgroundJobContainer(JdbcDatabaseContainer sqlContainer, Network network) {
        super("jobrunr-e2e-postgres:1.0", sqlContainer, network);
    }

    @Override
    protected StorageProvider initStorageProvider(JdbcDatabaseContainer sqlContainer) {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(sqlContainer.getJdbcUrl());
        dataSource.setUser(sqlContainer.getUsername());
        dataSource.setPassword(sqlContainer.getPassword());

        return SqlStorageProviderFactory.using(dataSource);
    }

}
