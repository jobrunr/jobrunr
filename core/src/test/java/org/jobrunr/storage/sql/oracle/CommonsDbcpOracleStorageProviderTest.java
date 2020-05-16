package org.jobrunr.storage.sql.oracle;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.executioncondition.RunTestBetween;
import org.junit.jupiter.executioncondition.RunTestIfDockerImageExists;

import javax.sql.DataSource;

@RunTestBetween(from = "03:00", to = "07:00")
@RunTestIfDockerImageExists("container-registry.oracle.com/database/standard:12.1.0.2")
class CommonsDbcpOracleStorageProviderTest extends AbstractOracleStorageProviderTest {

    //    docker run -d --env DB_PASSWD=oracle -p 1527:1521 -p 5507:5500 -it --shm-size="8g" container-registry.oracle.com/database/standard:12.1.0.2

    private static BasicDataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        if (dataSource == null) {
//            dataSource = new BasicDataSource();
//            dataSource.setUrl("jdbc:oracle:thin:@localhost:1527:xe".replace(":xe", ":ORCL"));
//            dataSource.setUsername("system");
//            dataSource.setPassword("oracle");

            System.out.println("==========================================================================================");
            System.out.println(sqlContainer.getLogs());
            System.out.println("==========================================================================================");

            dataSource = new BasicDataSource();

            dataSource.setUrl(sqlContainer.getJdbcUrl().replace(":xe", ":ORCL"));
            dataSource.setUsername(sqlContainer.getUsername());
            dataSource.setPassword(sqlContainer.getPassword());
        }

        return dataSource;
    }
}