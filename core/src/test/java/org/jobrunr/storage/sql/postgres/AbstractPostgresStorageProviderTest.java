package org.jobrunr.storage.sql.postgres;

import org.jobrunr.storage.sql.SqlStorageProviderTest;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class AbstractPostgresStorageProviderTest extends SqlStorageProviderTest {

    protected static PostgreSQLContainer sqlContainer = new PostgreSQLContainer<>();

    static {
        sqlContainer.start();
    }
}
