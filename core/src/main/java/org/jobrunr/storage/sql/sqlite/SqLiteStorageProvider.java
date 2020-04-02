package org.jobrunr.storage.sql.sqlite;

import org.jobrunr.storage.sql.common.DefaultSqlStorageProvider;

import javax.sql.DataSource;

public class SqLiteStorageProvider extends DefaultSqlStorageProvider {

    public SqLiteStorageProvider(DataSource dataSource) {
        super(dataSource);
    }

}
