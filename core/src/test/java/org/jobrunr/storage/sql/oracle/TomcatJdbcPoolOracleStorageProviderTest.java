package org.jobrunr.storage.sql.oracle;

import oracle.jdbc.driver.OracleDriver;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.executioncondition.RunTestBetween;
import org.junit.jupiter.executioncondition.RunTestIfDockerImageExists;

@RunTestBetween(from = "00:00", to = "03:00")
@RunTestIfDockerImageExists("container-registry.oracle.com/database/standard:12.1.0.2")
class TomcatJdbcPoolOracleStorageProviderTest extends AbstractOracleStorageProviderTest {

    //    docker run -d --env DB_PASSWD=oracle -p 1527:1521 -p 5507:5500 -it --shm-size="8g" container-registry.oracle.com/database/standard:12.1.0.2

    private static DataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
//            dataSource = new DataSource();
//            dataSource.setDriverClassName(OracleDriver.class.getName());
//            dataSource.setUrl("jdbc:oracle:thin:@localhost:1527:xe".replace(":xe", ":ORCL"));
//            dataSource.setUsername("system");
//            dataSource.setPassword("oracle");
            System.out.println("==========================================================================================");
            System.out.println(sqlContainer.getLogs());
            System.out.println("==========================================================================================");

            dataSource = new DataSource();
            dataSource.setDriverClassName(OracleDriver.class.getName());
            dataSource.setUrl(sqlContainer.getJdbcUrl().replace(":xe", ":ORCL"));
            dataSource.setUsername(sqlContainer.getUsername());
            dataSource.setPassword(sqlContainer.getPassword());
        }

        return dataSource;
    }

    @AfterAll
    public static void destroyDatasource() {
        dataSource.close();
    }
}