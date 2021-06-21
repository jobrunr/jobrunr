package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public abstract class AbstractBackgroundJobSqlContainer extends AbstractBackgroundJobContainer {

    private final JdbcDatabaseContainer sqlContainer;
    private StorageProvider jobStorageProvider;

    public AbstractBackgroundJobSqlContainer(String name, JdbcDatabaseContainer sqlContainer) {
        super(name);
        this.sqlContainer = sqlContainer;
    }

    @Override
    public void start() {
        setupContainer();
        super.start();
    }

    protected void setupContainer() {
        this
                .dependsOn(sqlContainer)
                .withEnv("JOBRUNR_JDBC_URL", sqlContainer.getJdbcUrl())
                .withEnv("JOBRUNR_JDBC_USERNAME", sqlContainer.getUsername())
                .withEnv("JOBRUNR_JDBC_PASSWORD", sqlContainer.getPassword())
                .withNetworkMode("host")
                .waitingFor(Wait.forLogMessage(".*Background Job server is ready *\\n", 1));
    }

    public StorageProvider getStorageProviderForClient() {
        try {
            if (jobStorageProvider == null) {
                jobStorageProvider = initStorageProvider(sqlContainer);
            }
            return jobStorageProvider;
        } catch (Exception e) {
            throw new RuntimeException("Error creating StorageProvider", e);
        }
    }

    protected abstract StorageProvider initStorageProvider(JdbcDatabaseContainer sqlContainer) throws Exception;
}
