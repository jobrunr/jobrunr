package org.jobrunr.storage.sql.h2;

import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.storage.sql.common.DefaultSqlStorageProvider;
import org.jobrunr.storage.sql.common.db.dialect.H2Dialect;

import javax.sql.DataSource;

public class H2StorageProvider extends DefaultSqlStorageProvider {

    public H2StorageProvider(DataSource dataSource) {
        this(dataSource, DatabaseOptions.CREATE);
    }

    public H2StorageProvider(DataSource dataSource, String tablePrefix) {
        this(dataSource, tablePrefix, DatabaseOptions.CREATE);
    }

    public H2StorageProvider(DataSource dataSource, DatabaseOptions databaseOptions) {
        super(dataSource, new H2Dialect(), databaseOptions);
    }

    public H2StorageProvider(DataSource dataSource, String tablePrefix, DatabaseOptions databaseOptions) {
        super(dataSource, new H2Dialect(), tablePrefix, databaseOptions);
    }

}
