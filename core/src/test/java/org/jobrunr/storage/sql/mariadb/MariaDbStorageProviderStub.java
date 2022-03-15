package org.jobrunr.storage.sql.mariadb;

import javax.sql.DataSource;

public class MariaDbStorageProviderStub extends MariaDbStorageProvider {

    public MariaDbStorageProviderStub(DataSource dataSource) {
        super(dataSource);
    }
}
