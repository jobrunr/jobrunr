package org.jobrunr.storage.sql.oracle;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.executioncondition.RunTestBetween;
import org.junit.jupiter.executioncondition.RunTestIfDockerImageExists;

import javax.sql.DataSource;

@RunTestBetween(from = "03:00", to = "07:00")
@RunTestIfDockerImageExists("container-registry.oracle.com/database/standard:12.1.0.2")
class CommonsDbcpOracleStorageProviderTest extends AbstractOracleStorageProviderTest {

    //    docker run -d --env DB_PASSWD=oracle -p 1527:1521 -p 5507:5500 -it --shm-size="8g" container-registry.oracle.com/database/standard:12.1.0.2
//    @Override
//    protected DataSource getDataSource() {
//        return createDataSource("jdbc:oracle:thin:@localhost:1527:xe", "system", "oracle", "ORCL");
//    }

    @Override
    protected DataSource getDataSource() {
        System.out.println("==========================================================================================");
        System.out.println(sqlContainer.getLogs());
        System.out.println("==========================================================================================");

        BasicDataSource ds = new BasicDataSource();
        ds.setUrl(sqlContainer.getJdbcUrl().replace(":xe", ":ORCL"));
        ds.setUsername(sqlContainer.getUsername());
        ds.setPassword(sqlContainer.getPassword());
        return ds;
    }
}