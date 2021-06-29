package org.jobrunr.storage.sql.sqlserver;

import org.jobrunr.storage.sql.common.DefaultSqlStorageProvider;
import org.jobrunr.storage.sql.common.db.dialect.SQLServerDialect;

import javax.sql.DataSource;

public class SQLServerStorageProvider extends DefaultSqlStorageProvider {

    public SQLServerStorageProvider(DataSource dataSource) {
        this(dataSource, DatabaseOptions.CREATE);
    }

    public SQLServerStorageProvider(DataSource dataSource, String schemaName) {
        this(dataSource, schemaName, DatabaseOptions.CREATE);
    }

    public SQLServerStorageProvider(DataSource dataSource, DatabaseOptions databaseOptions) {
        super(dataSource, new SQLServerDialect(), databaseOptions);
    }

    public SQLServerStorageProvider(DataSource dataSource, String schemaName, DatabaseOptions databaseOptions) {
        super(dataSource, new SQLServerDialect(), schemaName, databaseOptions);
    }

}
