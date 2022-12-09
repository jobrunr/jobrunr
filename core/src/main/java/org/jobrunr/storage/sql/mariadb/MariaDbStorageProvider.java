package org.jobrunr.storage.sql.mariadb;

import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.storage.sql.common.DefaultSqlStorageProvider;
import org.jobrunr.storage.sql.common.db.dialect.AnsiDialect;
import org.jobrunr.utils.exceptions.Exceptions;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;

public class MariaDbStorageProvider extends DefaultSqlStorageProvider {

    public MariaDbStorageProvider(DataSource dataSource) {
        this(dataSource, DatabaseOptions.CREATE);
    }

    public MariaDbStorageProvider(DataSource dataSource, String tablePrefix) {
        this(dataSource, tablePrefix, DatabaseOptions.CREATE);
    }

    public MariaDbStorageProvider(DataSource dataSource, DatabaseOptions databaseOptions) {
        this(dataSource, null, databaseOptions);
    }

    public MariaDbStorageProvider(DataSource dataSource, String tablePrefix, DatabaseOptions databaseOptions) {
        super(dataSource, new AnsiDialect(), tablePrefix, databaseOptions);
        try(Connection connection = dataSource.getConnection()) {
            String driverName = connection.getMetaData().getDriverName();
            int driverMajorVersion = connection.getMetaData().getDriverMajorVersion();
            if(driverName.toLowerCase().contains("mariadb") && driverMajorVersion >= 3) {
                String url = connection.getMetaData().getURL();
                if(!url.contains("useBulkStmts=false")) {
                    throw new IllegalStateException("JobRunr requires a MariaDB connection with useBulkStmts=false as otherwise optimistic locking cannot be validated.");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not query MariaDB driver", e);
        }
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
