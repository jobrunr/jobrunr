package org.jobrunr.storage.sql.mariadb;

import org.jobrunr.storage.sql.SqlStorageProviderTest;
import org.mariadb.jdbc.MariaDbPoolDataSource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.SQLException;

@Testcontainers
class MariaDbStorageProviderTest extends SqlStorageProviderTest {

    @Container
    private static MariaDBContainer sqlContainer = (MariaDBContainer) new MariaDBContainer().withEnv("TZ", "UTC");

    @Override
    protected DataSource getDataSource() {
        return createDataSource(sqlContainer.getJdbcUrl(), sqlContainer.getUsername(), sqlContainer.getPassword());
    }

    private static DataSource createDataSource(String url, String userName, String password) {
        try {
            MariaDbPoolDataSource dataSource = new MariaDbPoolDataSource();
            dataSource.setUrl(url + "?rewriteBatchedStatements=true&pool=true");
            dataSource.setUser(userName);
            dataSource.setPassword(password);
            return dataSource;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}