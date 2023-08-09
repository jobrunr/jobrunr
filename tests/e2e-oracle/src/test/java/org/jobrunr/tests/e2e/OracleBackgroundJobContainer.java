package org.jobrunr.tests.e2e;

import oracle.jdbc.pool.OracleDataSource;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.Network;

public class OracleBackgroundJobContainer extends AbstractBackgroundJobSqlContainer {

    public OracleBackgroundJobContainer(JdbcDatabaseContainer sqlContainer, Network network) {
        super("jobrunr-e2e-oracle:1.0", sqlContainer, network);
    }

    @Override
    protected StorageProvider initStorageProvider(JdbcDatabaseContainer sqlContainer) throws Exception {
        OracleDataSource dataSource = new OracleDataSource();
        dataSource.setURL(sqlContainer.getJdbcUrl().replace(":xe", ":ORCL"));
        dataSource.setUser(sqlContainer.getUsername());
        dataSource.setPassword(sqlContainer.getPassword());
        return SqlStorageProviderFactory.using(dataSource);
    }

}
