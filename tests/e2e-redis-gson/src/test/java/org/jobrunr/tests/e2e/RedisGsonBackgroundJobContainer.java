package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.redis.RedisStorageProvider;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import redis.clients.jedis.Jedis;

public class RedisGsonBackgroundJobContainer extends AbstractBackgroundJobContainer {

    private final GenericContainer redisContainer;

    public RedisGsonBackgroundJobContainer(GenericContainer redisContainer) {
        super("jobrunr-e2e-redis-gson:1.0");
        this.redisContainer = redisContainer;
    }

    @Override
    public void start() {
        Testcontainers.exposeHostPorts(redisContainer.getFirstMappedPort());
        this
                .dependsOn(redisContainer)
                .withEnv("REDIS_HOST", "host.testcontainers.internal")
                .withEnv("REDIS_PORT", String.valueOf(redisContainer.getFirstMappedPort()));

        super.start();
    }

    @Override
    public StorageProvider getStorageProviderForClient() {
        return new RedisStorageProvider(new Jedis(redisContainer.getContainerIpAddress(), redisContainer.getFirstMappedPort()));
    }

}
