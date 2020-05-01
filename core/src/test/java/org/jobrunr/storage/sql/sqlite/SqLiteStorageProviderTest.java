package org.jobrunr.storage.sql.sqlite;

import org.jobrunr.storage.sql.SqlStorageProviderTest;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;

class SqLiteStorageProviderTest extends SqlStorageProviderTest {

    @Override
    protected DataSource getDataSource() {
        SQLiteDataSource sqLiteDataSource = new SQLiteDataSource();
        sqLiteDataSource.setUrl("jdbc:sqlite:/tmp/jobrunr-test.db");
        return sqLiteDataSource;
    }
}