package org.jobrunr.storage.sql.h2;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public class HikariH2AutoCommitFalseStorageProviderTest extends HikariH2StorageProviderTest {

    private static HikariDataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            HikariConfig config = new HikariConfig();
            config.setAutoCommit(false);
            dataSource = createDataSource(config);
        }
        return dataSource;
    }
}
