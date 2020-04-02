package org.jobrunr.storage.sql.oracle;

import org.jobrunr.storage.sql.common.DefaultSqlStorageProvider;

import javax.sql.DataSource;

public class OracleStorageProvider extends DefaultSqlStorageProvider {

    public OracleStorageProvider(DataSource dataSource) {
        super(dataSource);
    }

}
