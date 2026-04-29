package org.jobrunr.storage.sql.postgres;

import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.storage.sql.common.DefaultSqlStorageProvider;
import org.jobrunr.utils.resilience.RateLimiter;

import javax.sql.DataSource;

import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;

public class PostgresStorageProvider extends DefaultSqlStorageProvider {

    public PostgresStorageProvider(DataSource dataSource) {
        this(dataSource, DatabaseOptions.CREATE);
    }

    public PostgresStorageProvider(DataSource dataSource, String tablePrefix) {
        this(dataSource, tablePrefix, DatabaseOptions.CREATE);
    }

    public PostgresStorageProvider(DataSource dataSource, DatabaseOptions databaseOptions) {
        this(dataSource, null, databaseOptions);
    }

    public PostgresStorageProvider(DataSource dataSource, String tablePrefix, DatabaseOptions databaseOptions) {
        this(dataSource, tablePrefix, databaseOptions, rateLimit().at1RequestPerSecond());
    }

    public PostgresStorageProvider(DataSource dataSource, String tablePrefix, DatabaseOptions databaseOptions, RateLimiter changeListenerNotificationRateLimit) {
        super(dataSource, new PostgresDialect(), tablePrefix, databaseOptions, changeListenerNotificationRateLimit);
    }

}
