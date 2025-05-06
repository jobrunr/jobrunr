package org.jobrunr.storage.sql.h2;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.h2.Driver;
import org.jobrunr.storage.sql.SqlStorageProviderTest;
import org.junit.jupiter.api.AfterAll;

import java.sql.SQLException;

public class TomcatJdbcPoolH2StorageProviderTest extends SqlStorageProviderTest {

    private static DataSource dataSource;

    @Override
    public DataSource getDataSource() {
        return getDataSource(true);
    }

    protected DataSource getDataSource(boolean autoCommit) {
        if (dataSource == null) {
            dataSource = new DataSource();
            dataSource.setDriverClassName(Driver.class.getName());
            dataSource.setUrl("jdbc:h2:mem:test-tomcat-jdbc;DB_CLOSE_DELAY=-1");
            dataSource.setUsername("sa");
            dataSource.setPassword("sa");
            dataSource.setDefaultAutoCommit(autoCommit);
        }
        return dataSource;
    }

    @AfterAll
    public static void destroyDatasource() throws SQLException {
        dataSource.close();
        dataSource = null;
    }
}
