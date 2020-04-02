package org.jobrunr.storage.sql.postgres;

import org.jobrunr.storage.sql.SqlStorageProviderTest;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;

@Testcontainers
class PostgresStorageProviderTest extends SqlStorageProviderTest {

    @Container
    private static PostgreSQLContainer sqlContainer = new PostgreSQLContainer<>();

    @Override
    protected DataSource getDataSource() {
        return createDataSource(sqlContainer.getJdbcUrl(), sqlContainer.getUsername(), sqlContainer.getPassword());
    }

    private static DataSource createDataSource(String url, String userName, String password) {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(url);
        dataSource.setUser(userName);
        dataSource.setPassword(password);
        return dataSource;
    }

}