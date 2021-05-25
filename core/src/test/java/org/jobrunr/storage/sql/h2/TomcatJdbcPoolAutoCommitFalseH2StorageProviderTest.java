package org.jobrunr.storage.sql.h2;

import org.apache.tomcat.jdbc.pool.DataSource;

public class TomcatJdbcPoolAutoCommitFalseH2StorageProviderTest extends TomcatJdbcPoolH2StorageProviderTest {

    @Override
    protected DataSource getDataSource() {
        return getDataSource(false);
    }
}
