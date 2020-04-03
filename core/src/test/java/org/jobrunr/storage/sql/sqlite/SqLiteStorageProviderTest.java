package org.jobrunr.storage.sql.sqlite;

import org.jobrunr.storage.sql.SqlStorageProviderTest;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;

class SqLiteStorageProviderTest extends SqlStorageProviderTest {

    @Override
    protected DataSource getDataSource() {
            return createDataSource("jdbc:sqlite:/tmp/jobrunr.db");
    }

    private static SQLiteDataSource createDataSource(String url) {
        SQLiteDataSource sqLiteDataSource = new SQLiteDataSource();
        sqLiteDataSource.setUrl(url);
        return sqLiteDataSource;
    }

}