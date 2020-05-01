package org.jobrunr.storage.sql.mariadb;

import org.jobrunr.storage.sql.SqlStorageProviderTest;
import org.testcontainers.containers.MariaDBContainer;

public abstract class AbstractMariaDbStorageProviderTest extends SqlStorageProviderTest {

    protected static MariaDBContainer sqlContainer = (MariaDBContainer) new MariaDBContainer().withEnv("TZ", "UTC");

    static {
        sqlContainer.start();
    }
}
