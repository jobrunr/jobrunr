package org.jobrunr.storage.sql.oracle;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.junit.jupiter.executioncondition.RunTestBetween;
import org.junit.jupiter.executioncondition.RunTestIfDockerImageExists;

import javax.sql.DataSource;

@RunTestBetween(from = "03:00", to = "07:00")
@RunTestIfDockerImageExists("container-registry.oracle.com/database/standard:12.1.0.2")
class C3p0OracleStorageProviderTest extends AbstractOracleStorageProviderTest {

    //    docker run -d --env DB_PASSWD=oracle -p 1527:1521 -p 5507:5500 -it --shm-size="8g" container-registry.oracle.com/database/standard:12.1.0.2
    private static ComboPooledDataSource dataSource;

    @Override
    protected DataSource getDataSource() {

        if (dataSource == null) {
//            dataSource = new ComboPooledDataSource();
//            dataSource.setJdbcUrl("jdbc:oracle:thin:@localhost:1527:xe".replace(":xe", ":ORCL"));
//            dataSource.setUser("system");
//            dataSource.setPassword("oracle");

            System.out.println("==========================================================================================");
            System.out.println(sqlContainer.getLogs());
            System.out.println("==========================================================================================");

            dataSource = new ComboPooledDataSource();
            dataSource.setJdbcUrl(sqlContainer.getJdbcUrl().replace(":xe", ":ORCL"));
            dataSource.setUser(sqlContainer.getUsername());
            dataSource.setPassword(sqlContainer.getPassword());
        }

        return dataSource;
    }
}