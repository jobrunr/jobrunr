package org.jobrunr.storage.sql.sqlite;

import org.jobrunr.storage.sql.SqlStorageProviderTest;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;

class SqLiteStorageProviderTest extends SqlStorageProviderTest {

    private static SQLiteDataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = new SQLiteDataSource();
            dataSource.setUrl("jdbc:sqlite:/tmp/jobrunr-test.db");
        }
        return dataSource;
    }
}