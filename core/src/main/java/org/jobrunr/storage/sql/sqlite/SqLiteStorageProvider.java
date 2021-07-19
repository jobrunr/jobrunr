package org.jobrunr.storage.sql.sqlite;

import org.jobrunr.storage.sql.common.DefaultSqlStorageProvider;
import org.jobrunr.storage.sql.common.db.dialect.AnsiDialect;

import javax.sql.DataSource;

import static org.jobrunr.utils.StringUtils.isNotNullOrEmpty;

public class SqLiteStorageProvider extends DefaultSqlStorageProvider {

    public SqLiteStorageProvider(DataSource dataSource) {
        this(dataSource, DatabaseOptions.CREATE);
    }

    public SqLiteStorageProvider(DataSource dataSource, String tablePrefix) {
        this(dataSource, DatabaseOptions.CREATE);
        if (isNotNullOrEmpty(tablePrefix)) {
            throw new IllegalArgumentException("SqLite does not support schema's.");
        }
    }

    public SqLiteStorageProvider(DataSource dataSource, DatabaseOptions databaseOptions) {
        super(dataSource, new AnsiDialect(), databaseOptions);
    }

    public SqLiteStorageProvider(DataSource dataSource, String tablePrefix, DatabaseOptions databaseOptions) {
        super(dataSource, new AnsiDialect(), databaseOptions);
        if (isNotNullOrEmpty(tablePrefix)) {
            throw new IllegalArgumentException("SqLite does not support schema's.");
        }
    }
}
