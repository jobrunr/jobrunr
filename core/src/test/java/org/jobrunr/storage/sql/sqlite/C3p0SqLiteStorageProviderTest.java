package org.jobrunr.storage.sql.sqlite;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.jobrunr.storage.sql.SqlStorageProviderTest;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

class C3p0SqLiteStorageProviderTest extends SqlStorageProviderTest {

    private static ComboPooledDataSource dataSource;

    @Override
    protected void cleanupDatabase(DataSource dataSource) {
        // strange bug in combination with C3p0 and Sqlite where although drop table succeeds, is still there
        super.cleanupDatabase(dataSource);
        super.cleanupDatabase(dataSource);
        super.cleanupDatabase(dataSource);
    }

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = new ComboPooledDataSource();
            dataSource.setJdbcUrl("jdbc:sqlite:/tmp/jobrunr-test.db");
        }
        return dataSource;
    }
}