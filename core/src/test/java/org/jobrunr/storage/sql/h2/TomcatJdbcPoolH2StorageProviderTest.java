package org.jobrunr.storage.sql.h2;

import org.jobrunr.storage.sql.SqlStorageProviderTest;

import javax.sql.DataSource;

public class TomcatJdbcPoolH2StorageProviderTest extends SqlStorageProviderTest {

    @Override
    protected DataSource getDataSource() {
        org.apache.tomcat.jdbc.pool.DataSource ds = new org.apache.tomcat.jdbc.pool.DataSource();
        ds.setUrl("jdbc:h2:/tmp/test");
        ds.setUsername("sa");
        ds.setPassword("sa");
        return ds;
    }
}