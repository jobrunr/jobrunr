package org.jobrunr.spring.autoconfigure.storage;

import io.lettuce.core.RedisClient;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.spring.autoconfigure.JobRunrAutoConfiguration;
import org.jobrunr.spring.autoconfigure.JobRunrProperties;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.redis.LettuceRedisStorageProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(before = JobRunrAutoConfiguration.class)
@ConditionalOnBean(RedisClient.class)
@ConditionalOnProperty(prefix = "org.jobrunr.database", name = "type", havingValue = "redis-lettuce", matchIfMissing = true)
public class JobRunrLettuceRedisClientStorageAutoConfiguration {

    @Bean(name = "storageProvider", destroyMethod = "close")
    @ConditionalOnMissingBean
    public StorageProvider lettuceRedisClientStorageProvider(RedisClient redisClient, JobMapper jobMapper, JobRunrProperties properties) {
        LettuceRedisStorageProvider lettuceRedisStorageProvider = new LettuceRedisStorageProvider(redisClient, properties.getDatabase().getTablePrefix());
        lettuceRedisStorageProvider.setJobMapper(jobMapper);
        return lettuceRedisStorageProvider;
    }
}
