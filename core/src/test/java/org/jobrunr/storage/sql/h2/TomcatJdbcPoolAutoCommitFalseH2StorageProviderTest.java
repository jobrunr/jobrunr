package org.jobrunr.storage.sql.h2;

import org.apache.tomcat.jdbc.pool.DataSource;

public class TomcatJdbcPoolAutoCommitFalseH2StorageProviderTest extends TomcatJdbcPoolH2StorageProviderTest {

    private static DataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = createDataSource();
            dataSource.setDefaultAutoCommit(false);
        }
        return dataSource;
    }
}
