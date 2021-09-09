package org.jobrunr.storage.sql.oracle;

import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.storage.sql.common.DefaultSqlStorageProvider;
import org.jobrunr.storage.sql.common.db.dialect.OracleDialect;

import javax.sql.DataSource;

public class OracleStorageProvider extends DefaultSqlStorageProvider {

    public OracleStorageProvider(DataSource dataSource) {
        this(dataSource, DatabaseOptions.CREATE);
    }

    public OracleStorageProvider(DataSource dataSource, String tablePrefix) {
        this(dataSource, tablePrefix, DatabaseOptions.CREATE);
    }

    public OracleStorageProvider(DataSource dataSource, DatabaseOptions databaseOptions) {
        super(dataSource, new OracleDialect(), databaseOptions);
    }

    public OracleStorageProvider(DataSource dataSource, String tablePrefix, DatabaseOptions databaseOptions) {
        super(dataSource, new OracleDialect(), tablePrefix, databaseOptions);
    }

}
