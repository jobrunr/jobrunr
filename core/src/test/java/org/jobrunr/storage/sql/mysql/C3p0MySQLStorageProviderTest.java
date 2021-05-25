package org.jobrunr.storage.sql.mysql;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.junit.jupiter.api.AfterAll;

import javax.sql.DataSource;

class C3p0MySQLStorageProviderTest extends AbstractMySQLStorageProviderTest {

    private static ComboPooledDataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = new ComboPooledDataSource();

            dataSource.setJdbcUrl(sqlContainer.getJdbcUrl() + "?rewriteBatchedStatements=true&useSSL=false");
            dataSource.setUser(sqlContainer.getUsername());
            dataSource.setPassword(sqlContainer.getPassword());
        }
        return dataSource;
    }

    @AfterAll
    public static void destroyDatasource() {
        dataSource.close();
    }
}