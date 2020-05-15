package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class RedisGsonE2ETest extends AbstractE2EGsonTest {

    @Container
    private static GenericContainer redisContainer = new GenericContainer("redis").withExposedPorts(6379);

    @Container
    private static RedisGsonBackgroundJobContainer backgroundJobServer = new RedisGsonBackgroundJobContainer(redisContainer);

    @Override
    protected StorageProvider getStorageProviderForClient() {
        return backgroundJobServer.getStorageProviderForClient();
    }

    @Override
    protected AbstractBackgroundJobContainer backgroundJobServer() {
        return backgroundJobServer;
    }
}
