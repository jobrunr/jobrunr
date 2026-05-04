package org.jobrunr.storage.sql.sqlserver;

import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.storage.sql.common.DefaultSqlStorageProvider;
import org.jobrunr.utils.resilience.RateLimiter;

import javax.sql.DataSource;

import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;

public class SQLServerStorageProvider extends DefaultSqlStorageProvider {

    public SQLServerStorageProvider(DataSource dataSource) {
        this(dataSource, DatabaseOptions.CREATE);
    }

    public SQLServerStorageProvider(DataSource dataSource, String tablePrefix) {
        this(dataSource, tablePrefix, DatabaseOptions.CREATE);
    }

    public SQLServerStorageProvider(DataSource dataSource, DatabaseOptions databaseOptions) {
        this(dataSource, null, databaseOptions);
    }

    public SQLServerStorageProvider(DataSource dataSource, String tablePrefix, DatabaseOptions databaseOptions) {
        this(dataSource, tablePrefix, databaseOptions, rateLimit().at1RequestPerSecond());
    }

    public SQLServerStorageProvider(DataSource dataSource, String tablePrefix, DatabaseOptions databaseOptions, RateLimiter changeListenerNotificationRateLimit) {
        super(dataSource, new SQLServerDialect(), tablePrefix, databaseOptions, changeListenerNotificationRateLimit);
    }

}
