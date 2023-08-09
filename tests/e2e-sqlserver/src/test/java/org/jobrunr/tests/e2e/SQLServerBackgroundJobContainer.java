package org.jobrunr.tests.e2e;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.Network;

public class SQLServerBackgroundJobContainer extends AbstractBackgroundJobSqlContainer {

    public SQLServerBackgroundJobContainer(JdbcDatabaseContainer sqlContainer, Network network) {
        super("jobrunr-e2e-sqlserver:1.0", sqlContainer, network);
    }

    @Override
    protected StorageProvider initStorageProvider(JdbcDatabaseContainer sqlContainer) {
        SQLServerDataSource dataSource = new SQLServerDataSource();
        dataSource.setURL(sqlContainer.getJdbcUrl());
        dataSource.setUser(sqlContainer.getUsername());
        dataSource.setPassword(sqlContainer.getPassword());

        return SqlStorageProviderFactory.using(dataSource);
    }

}
