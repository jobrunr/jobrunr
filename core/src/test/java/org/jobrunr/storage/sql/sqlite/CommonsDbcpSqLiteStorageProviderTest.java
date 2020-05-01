package org.jobrunr.storage.sql.sqlite;

import org.apache.commons.dbcp2.BasicDataSource;
import org.jobrunr.storage.sql.SqlStorageProviderTest;

import javax.sql.DataSource;

class CommonsDbcpSqLiteStorageProviderTest extends SqlStorageProviderTest {

    @Override
    protected DataSource getDataSource() {
        BasicDataSource ds = new BasicDataSource();
        ds.setUrl("jdbc:sqlite:/tmp/jobrunr-test.db");
        return ds;
    }
}