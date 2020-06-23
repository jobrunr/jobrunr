package org.jobrunr.storage.sql.sqlite;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.jobrunr.storage.sql.SqlStorageProviderTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;

import javax.sql.DataSource;

@Disabled("https://github.com/swaldman/c3p0/issues/147")
class C3p0SqLiteStorageProviderTest extends SqlStorageProviderTest {

    private static ComboPooledDataSource dataSource;

    /**
     * Strange bug when combining c3p0 and Sqlite where although drop table succeeds, the creation of table fails because it is still there.
     */
    protected void cleanupDatabase(DataSource dataSource) {
        try {
            super.cleanupDatabase(dataSource);
            Thread.sleep(20);
            super.cleanupDatabase(dataSource);
            Thread.sleep(20);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = new ComboPooledDataSource();
            dataSource.setJdbcUrl("jdbc:sqlite:/tmp/jobrunr-test.db");
        }
        return dataSource;
    }

    @AfterAll
    public static void destroyDatasource() {
        dataSource.close();
    }
}