package org.jobrunr.storage.sql.mysql;

import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.storage.sql.common.DefaultSqlStorageProvider;
import org.jobrunr.storage.sql.common.db.dialect.AnsiDialect;
import org.jobrunr.utils.exceptions.Exceptions;

import javax.sql.DataSource;
import java.time.Instant;

public class MySqlStorageProvider extends DefaultSqlStorageProvider {

    public MySqlStorageProvider(DataSource dataSource) {
        this(dataSource, DatabaseOptions.CREATE);
    }

    public MySqlStorageProvider(DataSource dataSource, String tablePrefix) {
        this(dataSource, tablePrefix, DatabaseOptions.CREATE);
    }

    public MySqlStorageProvider(DataSource dataSource, DatabaseOptions databaseOptions) {
        this(dataSource, null, databaseOptions);
    }

    public MySqlStorageProvider(DataSource dataSource, String tablePrefix, DatabaseOptions databaseOptions) {
        super(dataSource, new AnsiDialect(), tablePrefix, databaseOptions);
    }

    @Override
    public void announceBackgroundJobServer(BackgroundJobServerStatus serverStatus) {
        // why https://github.com/jobrunr/jobrunr/issues/635
        Exceptions.retryOnException(() -> super.announceBackgroundJobServer(serverStatus), 5);

    }

    @Override
    public int removeTimedOutBackgroundJobServers(Instant heartbeatOlderThan) {
        //why: https://github.com/jobrunr/jobrunr/issues/275
        return Exceptions.retryOnException(() -> super.removeTimedOutBackgroundJobServers(heartbeatOlderThan), 5);
    }

    @Override
    public boolean signalBackgroundJobServerAlive(BackgroundJobServerStatus serverStatus) {
        //why: https://github.com/jobrunr/jobrunr/issues/635
        return Exceptions.retryOnException(() -> super.signalBackgroundJobServerAlive(serverStatus), 5);
    }

    @Override
    public void signalBackgroundJobServerStopped(BackgroundJobServerStatus serverStatus) {
        // why https://github.com/jobrunr/jobrunr/issues/635
        Exceptions.retryOnException(() -> super.signalBackgroundJobServerStopped(serverStatus), 5);
    }
}
