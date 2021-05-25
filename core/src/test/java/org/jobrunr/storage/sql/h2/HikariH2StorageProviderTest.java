package org.jobrunr.storage.sql.h2;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jobrunr.storage.sql.SqlStorageProviderTest;
import org.junit.jupiter.api.AfterAll;

import javax.sql.DataSource;

public class HikariH2StorageProviderTest extends SqlStorageProviderTest {

    private static HikariDataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            HikariConfig config = new HikariConfig();
            dataSource = createDataSource(config);
        }
        return dataSource;
    }

    protected HikariDataSource createDataSource(HikariConfig config) {
        config.setJdbcUrl("jdbc:h2:/tmp/test-hikari");
        config.setUsername("sa");
        config.setPassword("sa");
        return new HikariDataSource(config);
    }

    @AfterAll
    public static void destroyDatasource() {
        dataSource.close();
    }
}
