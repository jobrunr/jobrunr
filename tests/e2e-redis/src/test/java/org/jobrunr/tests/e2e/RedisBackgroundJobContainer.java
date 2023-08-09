package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.redis.JedisRedisStorageProvider;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import redis.clients.jedis.JedisPool;

public class RedisBackgroundJobContainer extends AbstractBackgroundJobContainer {

    private final GenericContainer redisContainer;

    public RedisBackgroundJobContainer(GenericContainer redisContainer, Network network) {
        super("jobrunr-e2e-redis:1.0");
        this.redisContainer = redisContainer;
        this.withNetwork(network);
    }

    @Override
    public void start() {
        this
                .dependsOn(redisContainer)
                .withEnv("REDIS_HOST", redisContainer.getNetworkAliases().get(0).toString())
                .withEnv("REDIS_PORT", String.valueOf(6379));

        super.start();
    }

    @Override
    public StorageProvider getStorageProviderForClient() {
        return new JedisRedisStorageProvider(new JedisPool(redisContainer.getHost(), redisContainer.getFirstMappedPort()));
    }

}
