package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class RedisJacksonE2ETest extends AbstractE2EJacksonTest {

    private static final Network network = Network.newNetwork();

    @Container
    private static final GenericContainer redisContainer = new GenericContainer("redis")
            .withNetwork(network)
            .withNetworkAliases("redis")
            .withExposedPorts(6379);

    @Container
    private static final RedisJacksonBackgroundJobContainer backgroundJobServer = new RedisJacksonBackgroundJobContainer(redisContainer, network);

    @Override
    protected StorageProvider getStorageProviderForClient() {
        return backgroundJobServer.getStorageProviderForClient();
    }

    @Override
    protected AbstractBackgroundJobContainer backgroundJobServer() {
        return backgroundJobServer;
    }
}
