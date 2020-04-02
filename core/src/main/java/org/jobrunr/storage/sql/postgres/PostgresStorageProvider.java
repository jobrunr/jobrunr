package org.jobrunr.storage.sql.postgres;

import org.jobrunr.storage.sql.common.DefaultSqlStorageProvider;

import javax.sql.DataSource;

public class PostgresStorageProvider extends DefaultSqlStorageProvider {

    public PostgresStorageProvider(DataSource dataSource) {
        super(dataSource);
    }

}
