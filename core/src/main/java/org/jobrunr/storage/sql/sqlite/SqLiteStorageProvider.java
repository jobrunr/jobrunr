package org.jobrunr.storage.sql.sqlite;

import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.storage.sql.common.DefaultSqlStorageProvider;
import org.jobrunr.storage.sql.common.db.AnsiDialect;
import org.jobrunr.utils.resilience.RateLimiter;

import javax.sql.DataSource;

import static org.jobrunr.utils.StringUtils.isNotNullOrEmpty;
import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;

public class SqLiteStorageProvider extends DefaultSqlStorageProvider {

    public SqLiteStorageProvider(DataSource dataSource) {
        this(dataSource, DatabaseOptions.CREATE);
    }

    public SqLiteStorageProvider(DataSource dataSource, String tablePrefix) {
        this(dataSource, tablePrefix, DatabaseOptions.CREATE);
    }

    public SqLiteStorageProvider(DataSource dataSource, DatabaseOptions databaseOptions) {
        this(dataSource, null, databaseOptions);
    }

    public SqLiteStorageProvider(DataSource dataSource, String tablePrefix, DatabaseOptions databaseOptions) {
        this(dataSource, tablePrefix, databaseOptions, rateLimit().at1RequestPerSecond());
    }

    public SqLiteStorageProvider(DataSource dataSource, String tablePrefix, DatabaseOptions databaseOptions, RateLimiter changeListenerNotificationRateLimit) {
        super(dataSource, new AnsiDialect(), databaseOptions, changeListenerNotificationRateLimit);
        if (isNotNullOrEmpty(tablePrefix)) {
            throw new IllegalArgumentException("SqLite does not support schema's.");
        }
    }
}
