package org.jobrunr.storage.sql.h2;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import javax.sql.DataSource;

public class C3p0AutoCommitFalseH2StorageProviderTest extends C3p0H2StorageProviderTest {

    private static ComboPooledDataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = createDataSource();
            dataSource.setAutoCommitOnClose(false);
        }
        return dataSource;
    }
}
