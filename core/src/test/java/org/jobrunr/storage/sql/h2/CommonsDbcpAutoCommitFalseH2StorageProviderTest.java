package org.jobrunr.storage.sql.h2;

import org.apache.commons.dbcp2.BasicDataSource;

import javax.sql.DataSource;

public class CommonsDbcpAutoCommitFalseH2StorageProviderTest extends CommonsDbcpH2StorageProviderTest {

    private static BasicDataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = createDataSource();
            dataSource.setDefaultAutoCommit(false);
        }
        return dataSource;
    }
}
