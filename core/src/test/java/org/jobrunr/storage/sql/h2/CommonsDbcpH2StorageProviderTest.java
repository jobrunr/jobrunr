package org.jobrunr.storage.sql.h2;

import org.apache.commons.dbcp2.BasicDataSource;
import org.jobrunr.storage.sql.SqlStorageProviderTest;

import javax.sql.DataSource;

public class CommonsDbcpH2StorageProviderTest extends SqlStorageProviderTest {

    private static BasicDataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = new BasicDataSource();
            dataSource.setUrl("jdbc:h2:/tmp/test-commonsdbcp");
            dataSource.setUsername("sa");
            dataSource.setPassword("sa");
        }
        return dataSource;
    }
}