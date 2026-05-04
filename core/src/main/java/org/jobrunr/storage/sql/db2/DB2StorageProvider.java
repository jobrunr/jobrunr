package org.jobrunr.storage.sql.db2;

import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.storage.sql.common.DefaultSqlStorageProvider;
import org.jobrunr.utils.resilience.RateLimiter;

import javax.sql.DataSource;

import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;

public class DB2StorageProvider extends DefaultSqlStorageProvider {

    public DB2StorageProvider(DataSource dataSource) {
        this(dataSource, DatabaseOptions.CREATE);
    }

    public DB2StorageProvider(DataSource dataSource, String tablePrefix) {
        this(dataSource, tablePrefix, DatabaseOptions.CREATE);
    }

    public DB2StorageProvider(DataSource dataSource, DatabaseOptions databaseOptions) {
        this(dataSource, null, databaseOptions);
    }

    public DB2StorageProvider(DataSource dataSource, String tablePrefix, DatabaseOptions databaseOptions) {
        this(dataSource, tablePrefix, databaseOptions, rateLimit().at1RequestPerSecond());
    }

    public DB2StorageProvider(DataSource dataSource, String tablePrefix, DatabaseOptions databaseOptions, RateLimiter changeListenerNotificationRateLimit) {
        super(dataSource, new DB2Dialect(), tablePrefix, databaseOptions, changeListenerNotificationRateLimit);
    }

}
