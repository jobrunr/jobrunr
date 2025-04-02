package org.jobrunr.storage.sql.h2;

import com.zaxxer.hikari.HikariDataSource;

public class HikariH2AutoCommitFalseStorageProviderTest extends HikariH2StorageProviderTest {

    @Override
    public HikariDataSource getDataSource() {
        return getDataSource(false);
    }
}
