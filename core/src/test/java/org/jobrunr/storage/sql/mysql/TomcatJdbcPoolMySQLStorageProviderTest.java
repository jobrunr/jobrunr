package org.jobrunr.storage.sql.mysql;

import org.apache.tomcat.jdbc.pool.DataSource;

class TomcatJdbcPoolMySQLStorageProviderTest extends AbstractMySQLStorageProviderTest {

    private static DataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = new DataSource();
            dataSource.setUrl(sqlContainer.getJdbcUrl() + "?rewriteBatchedStatements=true");
            dataSource.setUsername(sqlContainer.getUsername());
            dataSource.setPassword(sqlContainer.getPassword());
        }
        return dataSource;
    }
}