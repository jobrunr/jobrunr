package org.jobrunr.storage.sql.mariadb;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.mariadb.jdbc.Driver;

class TomcatJdbcPoolMariaDbStorageProviderTest extends AbstractMariaDbStorageProviderTest {

    private static DataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = new DataSource();
            dataSource.setDriverClassName(Driver.class.getName());
            dataSource.setUrl(sqlContainer.getJdbcUrl() + "?rewriteBatchedStatements=true");
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