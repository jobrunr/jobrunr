package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.common.SqlJobStorageProviderFactory;
import org.postgresql.ds.PGSimpleDataSource;

public class Main extends AbstractMain {

    private static volatile Main main;

    public static void main(String[] args) throws Exception {
        if (main != null) return;
        main = new Main(args);
    }

    public Main(String[] args) throws Exception {
        super(args);
    }

    @Override
    protected StorageProvider initStorageProvider() {
        if (getEnvOrProperty("JOBRUNR_JDBC_URL") == null) {
            throw new IllegalStateException("Cannot start BackgroundJobServer: environment variable JOBRUNR_JDBC_URL is not set");
        }

        final PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(getEnvOrProperty("JOBRUNR_JDBC_URL"));
        dataSource.setUser(getEnvOrProperty("JOBRUNR_JDBC_USERNAME"));
        dataSource.setPassword(getEnvOrProperty("JOBRUNR_JDBC_PASSWORD"));
        return SqlJobStorageProviderFactory.using(dataSource);
    }
}
