package org.jobrunr.storage.sql.h2;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.security.NamePrincipal;
import io.agroal.api.security.SimplePassword;
import org.jobrunr.storage.sql.SqlStorageProviderTest;

import javax.sql.DataSource;
import java.sql.SQLException;

class AgroalH2StorageProviderTest extends SqlStorageProviderTest {

    private static AgroalDataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        try {
            if (dataSource == null) {
                AgroalDataSourceConfigurationSupplier configurationSupplier = new AgroalDataSourceConfigurationSupplier()
                        .connectionPoolConfiguration(cp -> cp
                                .maxSize(2)
                                .connectionFactoryConfiguration(cf -> cf
                                        .jdbcUrl("jdbc:h2:/tmp/test")
                                        .principal(new NamePrincipal("sa"))
                                        .credential(new SimplePassword("sa"))
                                )
                        );

                dataSource = AgroalDataSource.from(configurationSupplier);
            }
            return dataSource;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}