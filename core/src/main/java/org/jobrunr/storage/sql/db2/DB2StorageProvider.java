package org.jobrunr.storage.sql.db2;

import org.jobrunr.storage.sql.common.DefaultSqlStorageProvider;
import org.jobrunr.storage.sql.common.db.dialect.AnsiDialect;

import javax.sql.DataSource;

public class DB2StorageProvider extends DefaultSqlStorageProvider {

    public DB2StorageProvider(DataSource dataSource) {
        this(dataSource, DatabaseOptions.CREATE);
    }

    public DB2StorageProvider(DataSource dataSource, String schemaName) {
        this(dataSource, schemaName, DatabaseOptions.CREATE);
    }

    public DB2StorageProvider(DataSource dataSource, DatabaseOptions databaseOptions) {
        super(dataSource, new AnsiDialect(), databaseOptions);
    }

    public DB2StorageProvider(DataSource dataSource, String schemaName, DatabaseOptions databaseOptions) {
        super(dataSource, new AnsiDialect(), schemaName, databaseOptions);
    }

}
