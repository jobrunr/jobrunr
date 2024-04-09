package org.jobrunr.storage.sql.mysql;

import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.storage.sql.common.DefaultSqlStorageProvider;
import org.jobrunr.storage.sql.common.db.AnsiDialect;
import org.jobrunr.storage.sql.common.db.Dialect;
import org.jobrunr.utils.exceptions.Exceptions;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
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
        super(dataSource, getDialect(dataSource), tablePrefix, databaseOptions);
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

    private static Dialect getDialect(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            final String databaseProductName = connection.getMetaData().getDatabaseProductName();
            final String databaseProductVersion = connection.getMetaData().getDatabaseProductVersion();
            return new MySqlDialect(databaseProductName, databaseProductVersion);
        } catch (SQLException e) {
            // unable to determine DB version
        }
        return new AnsiDialect();
    }
}
