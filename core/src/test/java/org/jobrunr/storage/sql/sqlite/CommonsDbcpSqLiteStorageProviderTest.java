package org.jobrunr.storage.sql.sqlite;

import org.apache.commons.dbcp2.BasicDataSource;
import org.jobrunr.storage.sql.SqlStorageProviderTest;
import org.junit.jupiter.api.AfterAll;

import javax.sql.DataSource;
import java.sql.SQLException;

class CommonsDbcpSqLiteStorageProviderTest extends SqlStorageProviderTest {

    private static BasicDataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = new BasicDataSource();
            dataSource.setUrl("jdbc:sqlite:/tmp/jobrunr-test.db");
        }
        return dataSource;
    }

    @AfterAll
    public static void destroyDatasource() throws SQLException {
        dataSource.close();
    }
}