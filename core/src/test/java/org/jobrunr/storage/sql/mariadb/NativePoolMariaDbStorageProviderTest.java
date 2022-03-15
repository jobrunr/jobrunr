package org.jobrunr.storage.sql.mariadb;

import org.junit.jupiter.api.AfterAll;
import org.mariadb.jdbc.MariaDbPoolDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;

class NativePoolMariaDbStorageProviderTest extends AbstractMariaDbStorageProviderTest {

    private static MariaDbPoolDataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            try {
                dataSource = new MariaDbPoolDataSource();
                dataSource.setUrl(sqlContainer.getJdbcUrl() + "?rewriteBatchedStatements=true&pool=true&useBulkStmts=false");
                dataSource.setUser(sqlContainer.getUsername());
                dataSource.setPassword(sqlContainer.getPassword());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return dataSource;
    }

    @AfterAll
    public static void destroyDatasource() {
        dataSource.close();
    }
}