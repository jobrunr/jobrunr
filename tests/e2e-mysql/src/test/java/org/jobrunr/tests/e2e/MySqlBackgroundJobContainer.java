package org.jobrunr.tests.e2e;

import com.mysql.cj.jdbc.MysqlDataSource;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.Network;

public class MySqlBackgroundJobContainer extends AbstractBackgroundJobSqlContainer {

    public MySqlBackgroundJobContainer(JdbcDatabaseContainer sqlContainer, Network network) {
        super("jobrunr-e2e-mysql:1.0", sqlContainer, network);
    }

    @Override
    protected StorageProvider initStorageProvider(JdbcDatabaseContainer sqlContainer) {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setUrl(sqlContainer.getJdbcUrl() + "?rewriteBatchedStatements=true&pool=true");
        dataSource.setUser(sqlContainer.getUsername());
        dataSource.setPassword(sqlContainer.getPassword());
        return SqlStorageProviderFactory.using(dataSource);
    }

}
