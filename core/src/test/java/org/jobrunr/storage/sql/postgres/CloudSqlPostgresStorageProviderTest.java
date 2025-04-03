package org.jobrunr.storage.sql.postgres;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jobrunr.storage.sql.SqlStorageProviderTest;
import org.junit.jupiter.executioncondition.EnabledIfEnvVariableExists;

import javax.sql.DataSource;

/**
 * To run this test, the following environment variables must be set:
 * - GOOGLE_APPLICATION_CREDENTIALS=/path/to/service/account/key.json
 * - CLOUD_SQL_INSTANCE='<MY-PROJECT>:<INSTANCE-REGION>:<MY-DATABASE>'
 * - DB_USER='my-db-user'
 * - DB_PASS='my-db-pass'
 * - DB_NAME='my_db'
 */
@EnabledIfEnvVariableExists("GOOGLE_APPLICATION_CREDENTIALS")
class CloudSqlPostgresStorageProviderTest extends SqlStorageProviderTest {

    private static HikariDataSource dataSource;

    @Override
    public DataSource getDataSource() {
        if (dataSource == null) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(String.format("jdbc:postgresql:///%s", System.getenv("DB_NAME")));
            config.setUsername(System.getenv("DB_USER"));
            config.setPassword(System.getenv("DB_PASS"));
            config.addDataSourceProperty("socketFactory", "com.google.cloud.sql.postgres.SocketFactory");
            config.addDataSourceProperty("cloudSqlInstance", System.getenv("CLOUD_SQL_INSTANCE"));
            dataSource = new HikariDataSource(config);
        }

        return dataSource;
    }

}