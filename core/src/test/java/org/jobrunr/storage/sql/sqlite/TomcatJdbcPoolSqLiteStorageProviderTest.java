package org.jobrunr.storage.sql.sqlite;

import org.jobrunr.storage.sql.SqlStorageProviderTest;

import javax.sql.DataSource;

class TomcatJdbcPoolSqLiteStorageProviderTest extends SqlStorageProviderTest {

    @Override
    protected DataSource getDataSource() {
        org.apache.tomcat.jdbc.pool.DataSource ds = new org.apache.tomcat.jdbc.pool.DataSource();
        ds.setUrl("jdbc:sqlite:/tmp/jobrunr-test.db");
        return ds;
    }
}