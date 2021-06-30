package org.jobrunr.storage.nosql.redis;

import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderTest;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.AfterAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;

@Testcontainers
public class JedisRedisStorageProviderTest extends StorageProviderTest {

    @Container
    private static final GenericContainer redisContainer = new GenericContainer("redis").withExposedPorts(6379);

    private static JedisPool jedisPool;

    @Override
    protected void cleanup() {
        try (Jedis jedis = getJedisPool().getResource()) {
            jedis.flushDB();
        }
    }

    @Override
    protected StorageProvider getStorageProvider() {
        final JedisRedisStorageProvider jedisRedisStorageProvider = new JedisRedisStorageProvider(getJedisPool(), rateLimit().withoutLimits());
        jedisRedisStorageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
        return jedisRedisStorageProvider;
    }

    @AfterAll
    public static void shutdownJedisPool() {
        getJedisPool().close();
    }

    private static JedisPool getJedisPool() {
        if (jedisPool == null) {
            jedisPool = new JedisPool(redisContainer.getContainerIpAddress(), redisContainer.getMappedPort(6379));
        }
        return jedisPool;
    }
}
