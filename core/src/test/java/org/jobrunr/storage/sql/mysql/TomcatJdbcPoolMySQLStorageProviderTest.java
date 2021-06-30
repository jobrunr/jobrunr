package org.jobrunr.storage.sql.mysql;

import com.mysql.jdbc.Driver;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.junit.jupiter.api.AfterAll;

class TomcatJdbcPoolMySQLStorageProviderTest extends AbstractMySQLStorageProviderTest {

    private static DataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = new DataSource();
            dataSource.setDriverClassName(Driver.class.getName());
            dataSource.setUrl(sqlContainer.getJdbcUrl() + "?rewriteBatchedStatements=true&useSSL=false");
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