package org.jobrunr.storage.sql.sqlite;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.jobrunr.storage.sql.SqlStorageProviderTest;

import javax.sql.DataSource;

class C3p0SqLiteStorageProviderTest extends SqlStorageProviderTest {

    @Override
    protected DataSource getDataSource() {
        ComboPooledDataSource ds = new ComboPooledDataSource();
        ds.setJdbcUrl("jdbc:sqlite:/tmp/jobrunr-test.db");
        return ds;
    }
}