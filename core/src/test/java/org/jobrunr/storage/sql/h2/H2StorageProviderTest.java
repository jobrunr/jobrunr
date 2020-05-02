package org.jobrunr.storage.sql.h2;

import org.h2.jdbcx.JdbcDataSource;
import org.jobrunr.storage.sql.SqlStorageProviderTest;

import javax.sql.DataSource;

class H2StorageProviderTest extends SqlStorageProviderTest {

    private static JdbcDataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = new JdbcDataSource();
            dataSource.setURL("jdbc:h2:/tmp/test");
            dataSource.setUser("sa");
            dataSource.setPassword("sa");
        }
        return dataSource;
    }
}