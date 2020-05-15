package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class MongoDBGsonE2ETest extends AbstractE2EGsonTest {

    @Container
    private static GenericContainer mongoContainer = new GenericContainer("mongo").withExposedPorts(27017);

    @Container
    private static MongoDBGsonBackgroundJobContainer backgroundJobServer = new MongoDBGsonBackgroundJobContainer(mongoContainer);

    @Override
    protected StorageProvider getStorageProviderForClient() {
        return backgroundJobServer.getStorageProviderForClient();
    }

    @Override
    protected AbstractBackgroundJobContainer backgroundJobServer() {
        return backgroundJobServer;
    }
}
