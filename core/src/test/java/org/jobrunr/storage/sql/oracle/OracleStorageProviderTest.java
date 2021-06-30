package org.jobrunr.storage.sql.oracle;

import oracle.jdbc.pool.OracleDataSource;
import org.junit.jupiter.executioncondition.RunTestBetween;
import org.junit.jupiter.executioncondition.RunTestIfDockerImageExists;

import javax.sql.DataSource;
import java.sql.SQLException;

@RunTestBetween(from = "00:00", to = "03:00")
@RunTestIfDockerImageExists("container-registry.oracle.com/database/standard:12.1.0.2")
class OracleStorageProviderTest extends AbstractOracleStorageProviderTest {

    //    docker run -d --env DB_PASSWD=oracle -p 1527:1521 -p 5507:5500 -it --shm-size="8g" container-registry.oracle.com/database/standard:12.1.0.2

    private static OracleDataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        try {
            if (dataSource == null) {
//                dataSource = new OracleDataSource();
//                dataSource.setURL("jdbc:oracle:thin:@localhost:1527:xe".replace(":xe", ":ORCL"));
//                dataSource.setUser("system");
//                dataSource.setPassword("oracle");

                System.out.println("==========================================================================================");
                System.out.println(sqlContainer.getLogs());
                System.out.println("==========================================================================================");

                dataSource = new OracleDataSource();
                dataSource.setURL(sqlContainer.getJdbcUrl().replace(":xe", ":ORCL"));
                dataSource.setUser(sqlContainer.getUsername());
                dataSource.setPassword(sqlContainer.getPassword());
                dataSource.setServiceName("ORCL");
            }

            return dataSource;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}