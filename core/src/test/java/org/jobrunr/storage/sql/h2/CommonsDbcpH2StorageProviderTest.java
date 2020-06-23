package org.jobrunr.storage.sql.h2;

import org.apache.commons.dbcp2.BasicDataSource;
import org.jobrunr.storage.sql.SqlStorageProviderTest;
import org.junit.jupiter.api.AfterAll;

import javax.sql.DataSource;
import java.sql.SQLException;

public class CommonsDbcpH2StorageProviderTest extends SqlStorageProviderTest {

    private static BasicDataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = new BasicDataSource();
            dataSource.setUrl("jdbc:h2:/tmp/test-commonsdbcp");
            dataSource.setUsername("sa");
            dataSource.setPassword("sa");
        }
        return dataSource;
    }

    @AfterAll
    public static void destroyDatasource() throws SQLException {
        dataSource.close();
    }
}