package org.jobrunr.tests.e2e;

import oracle.jdbc.pool.OracleDataSource;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.common.SqlJobStorageProviderFactory;

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

        OracleDataSource dataSource = new OracleDataSource();
        dataSource.setURL(getEnvOrProperty("JOBRUNR_JDBC_URL").replace(":xe", ":ORCL"));
        dataSource.setUser(getEnvOrProperty("JOBRUNR_JDBC_USERNAME"));
        dataSource.setPassword(getEnvOrProperty("JOBRUNR_JDBC_PASSWORD"));
        dataSource.setServiceName("ORCL");
        return SqlJobStorageProviderFactory.using(dataSource);
    }
}
