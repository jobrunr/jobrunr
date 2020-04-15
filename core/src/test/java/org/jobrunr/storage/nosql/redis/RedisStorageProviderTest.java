package org.jobrunr.storage.nosql.redis;

import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderTest;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import redis.clients.jedis.Jedis;

public class RedisStorageProviderTest extends StorageProviderTest {

    @Override
    protected void cleanup() {
        try (Jedis jedis = new Jedis()) {
            jedis.flushDB();
        }
    }

    @Override
    protected StorageProvider getStorageProvider() {
        final RedisStorageProvider redisStorageProvider = new RedisStorageProvider();
        redisStorageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
        return redisStorageProvider;
    }
}
