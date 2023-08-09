package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory;

import javax.sql.DataSource;
import java.sql.SQLException;

public abstract class AbstractSqlMain extends AbstractMain {

    public AbstractSqlMain(String[] args) throws Exception {
        super(args);
    }

    protected abstract DataSource createDataSource(String jdbcUrl, String userName, String password) throws SQLException;

    protected StorageProvider initStorageProvider() throws Exception {
        if (getEnvOrProperty("JOBRUNR_JDBC_URL") == null) {
            throw new IllegalStateException("Cannot start BackgroundJobServer: environment variable JOBRUNR_JDBC_URL is not set");
        } else {
            System.out.println("Using JDBC Info:");
            System.out.println("JOBRUNR_JDBC_URL: " + getEnvOrProperty("JOBRUNR_JDBC_URL"));
            System.out.println("JOBRUNR_JDBC_USERNAME: " + getEnvOrProperty("JOBRUNR_JDBC_USERNAME"));
            System.out.println("JOBRUNR_JDBC_PASSWORD: " + getEnvOrProperty("JOBRUNR_JDBC_PASSWORD"));
        }

        DataSource dataSource = createDataSource(
                getEnvOrProperty("JOBRUNR_JDBC_URL"),
                getEnvOrProperty("JOBRUNR_JDBC_USERNAME"),
                getEnvOrProperty("JOBRUNR_JDBC_PASSWORD"));

        return SqlStorageProviderFactory.using(dataSource);
    }
}
