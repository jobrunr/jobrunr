package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.redis.JedisRedisStorageProvider;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import redis.clients.jedis.JedisPool;

public class RedisJacksonBackgroundJobContainer extends AbstractBackgroundJobContainer {

    private final GenericContainer redisContainer;
    private final Network network;

    public RedisJacksonBackgroundJobContainer(GenericContainer redisContainer, Network network) {
        super("jobrunr-e2e-redis-jackson:1.0");
        this.redisContainer = redisContainer;
        this.network = network;
    }

    @Override
    public void start() {
        this
                .dependsOn(redisContainer)
                .withNetwork(network)
                .withEnv("REDIS_HOST", "redis")
                .withEnv("REDIS_PORT", String.valueOf(6379));

        super.start();
    }

    @Override
    public StorageProvider getStorageProviderForClient() {
        return new JedisRedisStorageProvider(new JedisPool(redisContainer.getHost(), redisContainer.getFirstMappedPort()));
    }

}
