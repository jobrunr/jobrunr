package org.jobrunr.storage.sql.h2;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.jobrunr.storage.sql.SqlStorageProviderTest;

public class TomcatJdbcPoolH2StorageProviderTest extends SqlStorageProviderTest {

    private static DataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = new DataSource();
            dataSource.setUrl("jdbc:h2:/tmp/test");
            dataSource.setUsername("sa");
            dataSource.setPassword("sa");
        }
        return dataSource;
    }
}