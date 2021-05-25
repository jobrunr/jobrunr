package org.jobrunr.storage.sql.h2;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jobrunr.storage.sql.SqlStorageProviderTest;
import org.junit.jupiter.api.AfterAll;

public class HikariH2StorageProviderTest extends SqlStorageProviderTest {

    private static HikariDataSource dataSource;

    @Override
    protected HikariDataSource getDataSource() {
        return getDataSource(true);
    }

    protected HikariDataSource getDataSource(boolean autoCommit) {
        if (dataSource == null) {
            HikariConfig config = new HikariConfig();

            config.setJdbcUrl("jdbc:h2:/tmp/test-hikari");
            config.setUsername("sa");
            config.setPassword("sa");
            config.setAutoCommit(autoCommit);
            dataSource = new HikariDataSource(config);
        }
        return dataSource;
    }

    @AfterAll
    public static void destroyDatasource() {
        dataSource.close();
        dataSource = null;
    }
}
