package org.jobrunr.storage.sql.db2;

import org.jobrunr.storage.sql.common.DefaultSqlStorageProvider;

import javax.sql.DataSource;

public class DB2StorageProvider extends DefaultSqlStorageProvider {

    public DB2StorageProvider(DataSource dataSource) {
        super(dataSource);
    }

}
