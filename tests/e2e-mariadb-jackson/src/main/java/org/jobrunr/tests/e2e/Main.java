package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.common.SqlJobStorageProviderFactory;
import org.mariadb.jdbc.MariaDbPoolDataSource;

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
    protected StorageProvider initStorageProvider() throws Exception {
        if (getEnvOrProperty("JOBRUNR_JDBC_URL") == null) {
            throw new IllegalStateException("Cannot start BackgroundJobServer: environment variable JOBRUNR_JDBC_URL is not set");
        }

        MariaDbPoolDataSource dataSource = new MariaDbPoolDataSource();
        dataSource.setUrl(getEnvOrProperty("JOBRUNR_JDBC_URL") + "?rewriteBatchedStatements=true&pool=true");
        dataSource.setUser(getEnvOrProperty("JOBRUNR_JDBC_USERNAME"));
        dataSource.setPassword(getEnvOrProperty("JOBRUNR_JDBC_PASSWORD"));
        return SqlJobStorageProviderFactory.using(dataSource);
    }
}
