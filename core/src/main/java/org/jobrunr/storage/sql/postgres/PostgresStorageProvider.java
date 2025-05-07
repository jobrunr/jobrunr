package org.jobrunr.storage.sql.postgres;

import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.storage.sql.common.DefaultSqlStorageProvider;

import javax.sql.DataSource;

public class PostgresStorageProvider extends DefaultSqlStorageProvider {

    public PostgresStorageProvider(DataSource dataSource) {
        this(dataSource, DatabaseOptions.CREATE);
    }

    public PostgresStorageProvider(DataSource dataSource, String tablePrefix) {
        this(dataSource, tablePrefix, DatabaseOptions.CREATE);
    }

    public PostgresStorageProvider(DataSource dataSource, DatabaseOptions databaseOptions) {
        this(dataSource, null, databaseOptions);
    }

    public PostgresStorageProvider(DataSource dataSource, String tablePrefix, DatabaseOptions databaseOptions) {
        super(dataSource, new PostgresDialect(), tablePrefix, databaseOptions);
    }

}
