package org.jobrunr.storage.sql.h2;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.AfterAll;

import java.sql.SQLException;

public class CommonsDbcpH2StorageProviderTest extends AbstractH2StorageProviderTest {

    private static BasicDataSource dataSource;

    @Override
    public BasicDataSource getDataSource() {
        return getDataSource(true);
    }

    protected BasicDataSource getDataSource(boolean autoCommit) {
        if (dataSource == null) {
            dataSource = new BasicDataSource();
            dataSource.setUrl("jdbc:h2:mem:test-commons-db;DB_CLOSE_DELAY=-1");
            dataSource.setUsername("sa");
            dataSource.setPassword("sa");
            dataSource.setDefaultAutoCommit(autoCommit);
        }
        return dataSource;
    }

    @AfterAll
    public static void destroyDatasource() throws SQLException {
        shutdownDatabase(dataSource);
        dataSource.close();
        dataSource = null;
    }
}
