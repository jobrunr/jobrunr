package org.jobrunr.storage.sql.sqlite;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.jobrunr.storage.sql.SqlStorageProviderTest;

class TomcatJdbcPoolSqLiteStorageProviderTest extends SqlStorageProviderTest {

    private static DataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = new DataSource();
            dataSource.setUrl("jdbc:sqlite:/tmp/jobrunr-test.db");
        }
        return dataSource;
    }
}