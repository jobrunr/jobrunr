package org.jobrunr.spring.autoconfigure.storage;

import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.redis.JedisRedisStorageProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;

@Configuration
@ConditionalOnBean(JedisPool.class)
public class JobRunrJedisStorageAutoConfiguration {

    @Bean(name = "storageProvider", destroyMethod = "close")
    @ConditionalOnMissingBean
    public StorageProvider jedisStorageProvider(JedisPool jedisPool, JobMapper jobMapper) {
        JedisRedisStorageProvider jedisRedisStorageProvider = new JedisRedisStorageProvider(jedisPool);
        jedisRedisStorageProvider.setJobMapper(jobMapper);
        return jedisRedisStorageProvider;
    }
}
