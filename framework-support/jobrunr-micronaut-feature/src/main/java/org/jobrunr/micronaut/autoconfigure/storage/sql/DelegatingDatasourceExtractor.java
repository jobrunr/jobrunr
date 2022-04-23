package org.jobrunr.micronaut.autoconfigure.storage.sql;

import io.micronaut.transaction.jdbc.DelegatingDataSource;

import javax.sql.DataSource;

public class DelegatingDatasourceExtractor {

    private DelegatingDatasourceExtractor() {
    }

    public static DataSource extract(DataSource dataSource) {
        if(dataSource instanceof DelegatingDataSource) {
            return ((DelegatingDataSource) dataSource).getTargetDataSource();
        }
        return dataSource;
    }
}
