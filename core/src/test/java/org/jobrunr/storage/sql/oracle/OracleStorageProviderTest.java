package org.jobrunr.storage.sql.oracle;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.executioncondition.RunTestBetween;

import javax.sql.DataSource;

import static org.jobrunr.storage.sql.SqlTestUtils.toHikariDataSource;

@RunTestBetween(from = "00:00", to = "03:00")
class OracleStorageProviderTest extends AbstractOracleStorageProviderTest {

    //    docker run -d --env DB_PASSWD=oracle -p 1527:1521 -p 5507:5500 -it --shm-size="8g" container-registry.oracle.com/database/standard:12.1.0.2

    private static HikariDataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
            // dataSource = toHikariDataSource("jdbc:oracle:thin:@localhost:1527:xe".replace(":xe", ":ORCL"), "system", "oracle");

            System.out.println("==========================================================================================");
            System.out.println(sqlContainer.getLogs());
            System.out.println("==========================================================================================");

            dataSource = toHikariDataSource(sqlContainer.getJdbcUrl().replace(":xe", ":ORCL"), sqlContainer.getUsername(), sqlContainer.getPassword());
        }

        return dataSource;
    }

    @AfterAll
    public static void destroyDatasource() {
        dataSource.close();
        dataSource = null;
    }
}