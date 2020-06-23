package org.jobrunr.storage.sql.oracle;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.executioncondition.RunTestBetween;
import org.junit.jupiter.executioncondition.RunTestIfDockerImageExists;

import javax.sql.DataSource;

@RunTestBetween(from = "00:00", to = "03:00")
@RunTestIfDockerImageExists("container-registry.oracle.com/database/standard:12.1.0.2")
class HikariOracleStorageProviderTest extends AbstractOracleStorageProviderTest {

    //    docker run -d --env DB_PASSWD=oracle -p 1527:1521 -p 5507:5500 -it --shm-size="8g" container-registry.oracle.com/database/standard:12.1.0.2

    private static HikariDataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
//            HikariConfig config = new HikariConfig();
//            config.setJdbcUrl("jdbc:oracle:thin:@localhost:1527:xe".replace(":xe", ":ORCL"));
//            config.setUsername("system");
//            config.setPassword("oracle");
//            dataSource = new HikariDataSource(config);

            System.out.println("==========================================================================================");
            System.out.println(sqlContainer.getLogs());
            System.out.println("==========================================================================================");

            HikariConfig config = new HikariConfig();

            config.setJdbcUrl(sqlContainer.getJdbcUrl().replace(":xe", ":ORCL"));
            config.setUsername(sqlContainer.getUsername());
            config.setPassword(sqlContainer.getPassword());
            dataSource = new HikariDataSource(config);
        }

        return dataSource;
    }

    @AfterAll
    public static void destroyDatasource() {
        dataSource.close();
    }
}