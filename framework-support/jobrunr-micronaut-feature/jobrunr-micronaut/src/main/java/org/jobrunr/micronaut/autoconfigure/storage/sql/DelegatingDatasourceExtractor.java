package org.jobrunr.micronaut.autoconfigure.storage.sql;

import io.micronaut.data.connection.jdbc.advice.DelegatingDataSource;

import javax.sql.DataSource;

public class DelegatingDatasourceExtractor {

    private DelegatingDatasourceExtractor() {
    }

    public static DataSource extract(DataSource dataSource) {
        if (dataSource instanceof DelegatingDataSource delegatingDataSource) {
            return delegatingDataSource.getTargetDataSource();
        }
        return dataSource;
    }
}
