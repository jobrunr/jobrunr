package org.jobrunr.storage.sql.sqlite;

import org.jobrunr.storage.sql.SqlStorageProviderTest;
import org.junit.jupiter.api.AfterAll;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;

import static org.jobrunr.storage.sql.SqlTestUtils.deleteFile;

class SqLiteStorageProviderTest extends SqlStorageProviderTest {

    private static SQLiteDataSource dataSource;

    @Override
    public DataSource getDataSource() {
        if (dataSource == null) {
            deleteFile("/tmp/jobrunr-test.db");

            dataSource = new SQLiteDataSource();
            dataSource.setUrl("jdbc:sqlite:/tmp/jobrunr-test.db");
        }
        return dataSource;
    }

    @AfterAll
    public static void destroyDatasource() {
        dataSource = null;
    }
}