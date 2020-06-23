package org.jobrunr.storage.sql.h2;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.jobrunr.storage.sql.SqlStorageProviderTest;
import org.junit.jupiter.api.AfterAll;

import javax.sql.DataSource;

public class C3p0H2StorageProviderTest extends SqlStorageProviderTest {

    private static ComboPooledDataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = new ComboPooledDataSource();
            dataSource.setJdbcUrl("jdbc:h2:/tmp/test-c3p0");
            dataSource.setUser("sa");
            dataSource.setPassword("sa");
        }
        return dataSource;
    }

    @AfterAll
    public static void destroyDatasource() {
        dataSource.close();
    }
}