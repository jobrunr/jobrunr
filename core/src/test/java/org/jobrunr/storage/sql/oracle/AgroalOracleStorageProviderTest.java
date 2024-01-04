package org.jobrunr.storage.sql.oracle;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.security.NamePrincipal;
import io.agroal.api.security.SimplePassword;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.executioncondition.RunTestBetween;
import org.junit.jupiter.executioncondition.RunTestIfDockerImageExists;

import javax.sql.DataSource;
import java.sql.SQLException;

@RunTestBetween(from = "00:00", to = "03:00")
@RunTestIfDockerImageExists("container-registry.oracle.com/database/standard:12.1.0.2")
class AgroalOracleStorageProviderTest extends AbstractOracleStorageProviderTest {

    //    docker run -d --env DB_PASSWD=oracle -p 1527:1521 -p 5507:5500 -it --shm-size="8g" container-registry.oracle.com/database/standard:12.1.0.2

    private static AgroalDataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        try {
            if (dataSource == null) {
                System.out.println("==========================================================================================");
                System.out.println(sqlContainer.getLogs());
                System.out.println("==========================================================================================");

                AgroalDataSourceConfigurationSupplier configurationSupplier = new AgroalDataSourceConfigurationSupplier()
                        .connectionPoolConfiguration(cp -> cp
                                .maxSize(2)
                                .connectionFactoryConfiguration(cf -> cf
                                        .jdbcUrl(sqlContainer.getJdbcUrl().replace(":xe", ":ORCL"))
                                        .principal(new NamePrincipal(sqlContainer.getUsername()))
                                        .credential(new SimplePassword(sqlContainer.getPassword()))
                                )
                        );

                dataSource = AgroalDataSource.from(configurationSupplier);
            }
            return dataSource;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @AfterAll
    public static void destroyDatasource() {
        dataSource.close();
    }
}