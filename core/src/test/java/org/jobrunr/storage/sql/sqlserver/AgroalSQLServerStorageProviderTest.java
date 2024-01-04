package org.jobrunr.storage.sql.sqlserver;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.security.NamePrincipal;
import io.agroal.api.security.SimplePassword;
import org.junit.jupiter.api.AfterAll;

import javax.sql.DataSource;
import java.sql.SQLException;

class AgroalSQLServerStorageProviderTest extends AbstractSQLServerStorageProviderTest {

    private static AgroalDataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        try {
            if (dataSource == null) {
                AgroalDataSourceConfigurationSupplier configurationSupplier = new AgroalDataSourceConfigurationSupplier()
                        .connectionPoolConfiguration(cp -> cp
                                .maxSize(2)
                                .connectionFactoryConfiguration(cf -> cf
                                        .jdbcUrl(sqlContainer.getJdbcUrl())
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
    public static void destroyDatasource() throws SQLException {
        dataSource.close();
    }
}