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
import redis.clients.jedis.Transaction;
import redis.clients.jedis.exceptions.JedisException;

import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

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

    @Override
    protected ThrowingStorageProvider makeThrowingStorageProvider(StorageProvider storageProvider) {
        return new ThrowingJedisStorageProvider(storageProvider);
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

    public static class ThrowingJedisStorageProvider extends ThrowingStorageProvider {

        public ThrowingJedisStorageProvider(StorageProvider storageProvider) {
            super(storageProvider, "jedisPool");
        }

        @Override
        protected void makeStorageProviderThrowException(StorageProvider storageProvider) {
            JedisPool jedisPoolMock = mock(JedisPool.class);
            Jedis jedisMock = mock(Jedis.class);
            Transaction transactionMock = mock(Transaction.class);
            when(jedisPoolMock.getResource()).thenReturn(jedisMock);
            String versionMatcher = endsWith("version");
            when(jedisMock.get(versionMatcher)).thenReturn("1");
            when(jedisMock.multi()).thenReturn(transactionMock);
            when(jedisMock.unwatch()).thenThrow(new JedisException("some exception"));
            setInternalState(storageProvider, "jedisPool", jedisPoolMock);
        }
    }
}
