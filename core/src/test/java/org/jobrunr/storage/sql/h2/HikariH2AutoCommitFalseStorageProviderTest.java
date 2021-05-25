package org.jobrunr.storage.sql.h2;

import com.zaxxer.hikari.HikariDataSource;

public class HikariH2AutoCommitFalseStorageProviderTest extends HikariH2StorageProviderTest {

    @Override
    protected HikariDataSource getDataSource() {
        return getDataSource(false);
    }
}
