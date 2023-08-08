package org.jobrunr.tests.e2e;

import oracle.jdbc.pool.OracleDataSource;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;

public class OracleJacksonBackgroundJobContainer extends AbstractBackgroundJobSqlContainer {

    public OracleJacksonBackgroundJobContainer(JdbcDatabaseContainer sqlContainer) {
        super("jobrunr-e2e-oracle-jackson:1.0", sqlContainer);
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
