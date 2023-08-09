package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import static org.jobrunr.utils.StringUtils.isNotNullOrEmpty;

public abstract class AbstractBackgroundJobSqlContainer extends AbstractBackgroundJobContainer {

    private final JdbcDatabaseContainer sqlContainer;
    private StorageProvider jobStorageProvider;

    public AbstractBackgroundJobSqlContainer(String name, JdbcDatabaseContainer sqlContainer, Network network) {
        super(name);
        this.sqlContainer = sqlContainer;
        this.withNetwork(network);
    }

    @Override
    public void start() {
        setupContainer();
        super.start();
    }

    protected void setupContainer() {
        this
                .dependsOn(sqlContainer)
                .withEnv("JOBRUNR_JDBC_URL", getJdbcUrl(sqlContainer))
                .withEnv("JOBRUNR_JDBC_USERNAME", sqlContainer.getUsername())
                .withEnv("JOBRUNR_JDBC_PASSWORD", sqlContainer.getPassword())
                .waitingFor(Wait.forLogMessage(".*Background Job server is ready *\\n", 1));
    }

    protected String getJdbcUrl(JdbcDatabaseContainer sqlContainer) {
        String jdbcUrl = sqlContainer.getJdbcUrl()
                .replace("localhost", sqlContainer.getNetworkAliases().get(0).toString())
                .replace(String.valueOf(sqlContainer.getFirstMappedPort()), sqlContainer.getExposedPorts().get(0).toString());
        if (isNotNullOrEmpty(sqlContainer.getContainerIpAddress())) {
            jdbcUrl = jdbcUrl.replace(sqlContainer.getContainerIpAddress(), sqlContainer.getNetworkAliases().get(0).toString());
        }
        if (isNotNullOrEmpty(sqlContainer.getContainerInfo().getNetworkSettings().getIpAddress())) {
            jdbcUrl = jdbcUrl.replace(sqlContainer.getContainerInfo().getNetworkSettings().getIpAddress(), sqlContainer.getNetworkAliases().get(0).toString());
        }
        return jdbcUrl;
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
