package org.jobrunr.storage.sql.h2;

import org.h2.jdbcx.JdbcDataSource;
import org.jobrunr.storage.sql.SqlStorageProviderTest;

import javax.sql.DataSource;

class H2StorageProviderTest extends SqlStorageProviderTest {

    @Override
    protected DataSource getDataSource() {
        return createDataSource();
    }

    private static DataSource createDataSource() {
        final JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:/tmp/test");
        ds.setUser("sa");
        ds.setPassword("sa");
        return ds;
    }
}