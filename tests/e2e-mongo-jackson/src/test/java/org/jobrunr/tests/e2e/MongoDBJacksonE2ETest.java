package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class MongoDBJacksonE2ETest extends AbstractE2EJacksonTest {

    @Container
    private static GenericContainer mongoContainer = new GenericContainer("mongo").withExposedPorts(27017);

    @Container
    private static MongoDBJacksonBackgroundJobContainer backgroundJobServer = new MongoDBJacksonBackgroundJobContainer(mongoContainer);

    @Override
    protected StorageProvider getStorageProviderForClient() {
        return backgroundJobServer.getStorageProviderForClient();
    }

    @Override
    protected AbstractBackgroundJobContainer backgroundJobServer() {
        return backgroundJobServer;
    }
}
