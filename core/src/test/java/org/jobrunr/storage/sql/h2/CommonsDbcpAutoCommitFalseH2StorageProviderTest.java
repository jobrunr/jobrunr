package org.jobrunr.storage.sql.h2;

import org.apache.commons.dbcp2.BasicDataSource;

public class CommonsDbcpAutoCommitFalseH2StorageProviderTest extends CommonsDbcpH2StorageProviderTest {

    @Override
    protected BasicDataSource getDataSource() {
        return getDataSource(false);
    }
}
