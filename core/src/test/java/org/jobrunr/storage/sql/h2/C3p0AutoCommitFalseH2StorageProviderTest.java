package org.jobrunr.storage.sql.h2;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class C3p0AutoCommitFalseH2StorageProviderTest extends C3p0H2StorageProviderTest {

    @Override
    protected ComboPooledDataSource getDataSource() {
        return getDataSource(false);
    }
}
