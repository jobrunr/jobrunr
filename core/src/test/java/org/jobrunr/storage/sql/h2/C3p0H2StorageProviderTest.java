package org.jobrunr.storage.sql.h2;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.jobrunr.storage.sql.SqlStorageProviderTest;

import javax.sql.DataSource;

public class C3p0H2StorageProviderTest extends SqlStorageProviderTest {

    @Override
    protected DataSource getDataSource() {
        ComboPooledDataSource ds = new ComboPooledDataSource();
        ds.setJdbcUrl("jdbc:h2:/tmp/test");
        ds.setUser("sa");
        ds.setPassword("sa");
        return ds;
    }
}