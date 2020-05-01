package org.jobrunr.storage.sql.h2;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jobrunr.storage.sql.SqlStorageProviderTest;

import javax.sql.DataSource;

public class HikariH2StorageProviderTest extends SqlStorageProviderTest {

    @Override
    protected DataSource getDataSource() {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl("jdbc:h2:/tmp/test");
        config.setUsername("sa");
        config.setPassword("sa");
        return new HikariDataSource(config);
    }
}