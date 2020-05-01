package org.jobrunr.storage.sql.h2;

import org.apache.commons.dbcp2.BasicDataSource;
import org.jobrunr.storage.sql.SqlStorageProviderTest;

import javax.sql.DataSource;

public class CommonsDbcpH2StorageProviderTest extends SqlStorageProviderTest {

    @Override
    protected DataSource getDataSource() {
        BasicDataSource ds = new BasicDataSource();
        ds.setUrl("jdbc:h2:/tmp/test");
        ds.setUsername("sa");
        ds.setPassword("sa");
        return ds;
    }
}