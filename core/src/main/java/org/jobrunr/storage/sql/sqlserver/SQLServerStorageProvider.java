package org.jobrunr.storage.sql.sqlserver;

import org.jobrunr.storage.sql.common.DefaultSqlStorageProvider;

import javax.sql.DataSource;

public class SQLServerStorageProvider extends DefaultSqlStorageProvider {

    public SQLServerStorageProvider(DataSource dataSource) {
        super(dataSource);
    }

}
