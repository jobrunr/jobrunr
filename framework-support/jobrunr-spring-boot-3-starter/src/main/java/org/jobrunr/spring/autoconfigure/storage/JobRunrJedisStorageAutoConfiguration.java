package org.jobrunr.spring.autoconfigure.storage;

import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.spring.autoconfigure.JobRunrAutoConfiguration;
import org.jobrunr.spring.autoconfigure.JobRunrProperties;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.redis.JedisRedisStorageProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import redis.clients.jedis.JedisPool;

@AutoConfiguration(before = JobRunrAutoConfiguration.class)
@ConditionalOnBean(JedisPool.class)
@ConditionalOnProperty(prefix = "org.jobrunr.database", name = "type", havingValue = "redis-jedis", matchIfMissing = true)
public class JobRunrJedisStorageAutoConfiguration {

    @Bean(name = "storageProvider", destroyMethod = "close")
    @ConditionalOnMissingBean
    public StorageProvider jedisStorageProvider(JedisPool jedisPool, JobMapper jobMapper, JobRunrProperties properties) {
        JedisRedisStorageProvider jedisRedisStorageProvider = new JedisRedisStorageProvider(jedisPool, properties.getDatabase().getTablePrefix());
        jedisRedisStorageProvider.setJobMapper(jobMapper);
        return jedisRedisStorageProvider;
    }
}
