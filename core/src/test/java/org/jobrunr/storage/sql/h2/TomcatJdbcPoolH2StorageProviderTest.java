package org.jobrunr.storage.sql.h2;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.h2.Driver;
import org.jobrunr.storage.sql.SqlStorageProviderTest;
import org.junit.jupiter.api.AfterAll;

public class TomcatJdbcPoolH2StorageProviderTest extends SqlStorageProviderTest {

    private static DataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        return getDataSource(true);
    }

    protected DataSource getDataSource(boolean autoCommit) {
        if (dataSource == null) {
            dataSource = new DataSource();
            dataSource.setDriverClassName(Driver.class.getName());
            dataSource.setUrl("jdbc:h2:/tmp/test-tomcatjdbcpool");
            dataSource.setUsername("sa");
            dataSource.setPassword("sa");
            dataSource.setDefaultAutoCommit(autoCommit);
        }
        return dataSource;
    }

    @AfterAll
    public static void destroyDatasource() {
        dataSource.close();
        dataSource = null;
    }
}
