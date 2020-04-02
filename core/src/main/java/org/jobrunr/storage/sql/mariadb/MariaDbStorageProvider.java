package org.jobrunr.storage.sql.mariadb;

import org.jobrunr.storage.sql.common.DefaultSqlStorageProvider;

import javax.sql.DataSource;

public class MariaDbStorageProvider extends DefaultSqlStorageProvider {

    public MariaDbStorageProvider(DataSource dataSource) {
        super(dataSource);
    }

}
