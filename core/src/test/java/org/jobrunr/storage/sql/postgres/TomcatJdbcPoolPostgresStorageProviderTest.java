package org.jobrunr.storage.sql.postgres;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.postgresql.Driver;

class TomcatJdbcPoolPostgresStorageProviderTest extends AbstractPostgresStorageProviderTest {

    private static DataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = new DataSource();
            dataSource.setDriverClassName(Driver.class.getName());
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