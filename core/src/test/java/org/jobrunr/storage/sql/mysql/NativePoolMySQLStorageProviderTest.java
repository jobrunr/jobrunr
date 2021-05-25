package org.jobrunr.storage.sql.mysql;

import com.mysql.cj.jdbc.MysqlDataSource;

import javax.sql.DataSource;

class NativePoolMySQLStorageProviderTest extends AbstractMySQLStorageProviderTest {

    private static MysqlDataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = new MysqlDataSource();
            dataSource.setUrl(sqlContainer.getJdbcUrl() + "?rewriteBatchedStatements=true&pool=true&useSSL=false");
            dataSource.setUser(sqlContainer.getUsername());
            dataSource.setPassword(sqlContainer.getPassword());
        }
        return dataSource;
    }
}