package org.jobrunr.storage.sql.db2;

import com.ibm.db2.jcc.DB2Driver;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.junit.jupiter.api.AfterAll;

class TomcatJdbcPoolDB2StorageProviderTest extends AbstractDB2StorageProviderTest {

    private static DataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = new DataSource();
            dataSource.setDriverClassName(DB2Driver.class.getName());
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