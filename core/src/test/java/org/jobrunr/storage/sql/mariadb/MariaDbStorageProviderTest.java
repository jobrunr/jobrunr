package org.jobrunr.storage.sql.mariadb;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;

import javax.sql.DataSource;

import static org.jobrunr.storage.sql.SqlTestUtils.toHikariDataSource;

class MariaDbStorageProviderTest extends AbstractMariaDbStorageProviderTest {

    private static HikariDataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = toHikariDataSource(sqlContainer, "?rewriteBatchedStatements=true&useBulkStmts=false");
        }
        return dataSource;
    }

    @AfterAll
    public static void destroyDatasource() {
        dataSource.close();
        dataSource = null;
    }
}