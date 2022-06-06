package org.jobrunr.storage.sql.db2;

import com.ibm.db2.jcc.DB2SimpleDataSource;

import javax.sql.DataSource;

class DB2StorageProviderTest extends AbstractDB2StorageProviderTest {

    private static DB2SimpleDataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = new DB2SimpleDataSource();
            dataSource.setServerName(sqlContainer.getHost());
            dataSource.setUser(sqlContainer.getUsername());
            dataSource.setPassword(sqlContainer.getPassword());
            dataSource.setDatabaseName(sqlContainer.getDatabaseName());
            dataSource.setPortNumber(sqlContainer.getFirstMappedPort());
            dataSource.setDriverType(4);
        }
        return dataSource;
    }
}