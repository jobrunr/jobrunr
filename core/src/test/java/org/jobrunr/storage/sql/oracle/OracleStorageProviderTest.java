package org.jobrunr.storage.sql.oracle;

import oracle.jdbc.pool.OracleDataSource;
import org.jobrunr.storage.sql.SqlStorageProviderTest;
import org.junit.jupiter.executioncondition.RunTestBetween;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.SQLException;

@RunTestBetween(from = "03:00", to = "07:00")
@Testcontainers
class OracleStorageProviderTest extends SqlStorageProviderTest {

    @Container
    private static OracleContainer sqlContainer = new OracleContainer("container-registry.oracle.com/database/standard:12.1.0.2")
            .withStartupTimeoutSeconds(900)
            .withConnectTimeoutSeconds(500)
            .withEnv("DB_SID", "ORCL")
            .withEnv("DB_PASSWD", "oracle")
            .withSharedMemorySize(4294967296L);

    @Override
    protected DataSource getDataSource() {
        System.out.println("==========================================================================================");
        System.out.println(sqlContainer.getLogs());
        System.out.println("==========================================================================================");

        return createDataSource(sqlContainer.getJdbcUrl(), sqlContainer.getUsername(), sqlContainer.getPassword(), "ORCL");
    }

//    docker run -d --env DB_PASSWD=oracle -p 1527:1521 -p 5507:5500 -it --shm-size="8g" container-registry.oracle.com/database/standard:12.1.0.2
//    @Override
//    protected DataSource getDataSource() {
//        return createDataSource("jdbc:oracle:thin:@localhost:1527:xe", "system", "oracle", "ORCL");
//    }

    private static DataSource createDataSource(String url, String userName, String password, String serviceName) {
        try {
            OracleDataSource dataSource = new OracleDataSource();
            dataSource.setURL(url.replace(":xe", ":ORCL"));
            dataSource.setUser(userName);
            dataSource.setPassword(password);
            dataSource.setServiceName(serviceName);
            return dataSource;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}