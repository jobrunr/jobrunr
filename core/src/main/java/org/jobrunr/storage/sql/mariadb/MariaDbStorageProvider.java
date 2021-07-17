package org.jobrunr.storage.sql.mariadb;

import org.jobrunr.storage.sql.common.DefaultSqlStorageProvider;
import org.jobrunr.storage.sql.common.db.dialect.AnsiDialect;

import javax.sql.DataSource;

public class MariaDbStorageProvider extends DefaultSqlStorageProvider {

    public MariaDbStorageProvider(DataSource dataSource) {
        this(dataSource, DatabaseOptions.CREATE);
    }

    public MariaDbStorageProvider(DataSource dataSource, String tablePrefix) {
        this(dataSource, tablePrefix, DatabaseOptions.CREATE);
    }

    public MariaDbStorageProvider(DataSource dataSource, DatabaseOptions databaseOptions) {
        super(dataSource, new AnsiDialect(), databaseOptions);
    }

    public MariaDbStorageProvider(DataSource dataSource, String tablePrefix, DatabaseOptions databaseOptions) {
        super(dataSource, new AnsiDialect(), tablePrefix, databaseOptions);
    }

}
