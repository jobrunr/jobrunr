package org.jobrunr.storage.sql.sqlserver;

import com.microsoft.sqlserver.jdbc.SQLServerDriver;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.junit.jupiter.api.AfterAll;

class TomcatJdbcPoolSQLServerStorageProviderTest extends AbstractSQLServerStorageProviderTest {

    private static DataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = new DataSource();
            dataSource.setDriverClassName(SQLServerDriver.class.getName());
            dataSource.setUrl(sqlContainer.getJdbcUrl());
            dataSource.setUsername(sqlContainer.getUsername());
            dataSource.setPassword(sqlContainer.getPassword());
        }
        return dataSource;
    }

    @AfterAll
    public static void destroyDatasource() {
        dataSource.close();
    }
}