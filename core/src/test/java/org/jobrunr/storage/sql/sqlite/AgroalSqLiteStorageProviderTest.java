package org.jobrunr.storage.sql.sqlite;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import org.jobrunr.storage.sql.SqlStorageProviderTest;
import org.junit.jupiter.api.AfterAll;

import javax.sql.DataSource;
import java.sql.SQLException;

class AgroalSqLiteStorageProviderTest extends SqlStorageProviderTest {

    private static AgroalDataSource dataSource;

    @Override
    protected DataSource getDataSource() {
        try {
            if (dataSource == null) {
                deleteFile("/tmp/jobrunr-test.db");
                AgroalDataSourceConfigurationSupplier configurationSupplier = new AgroalDataSourceConfigurationSupplier()
                        .connectionPoolConfiguration(cp -> cp
                                .maxSize(2)
                                .connectionFactoryConfiguration(cf -> cf
                                        .jdbcUrl("jdbc:sqlite:/tmp/jobrunr-test.db")
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