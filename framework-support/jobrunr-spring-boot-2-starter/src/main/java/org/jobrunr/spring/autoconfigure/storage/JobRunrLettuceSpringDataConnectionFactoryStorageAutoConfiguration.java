package org.jobrunr.spring.autoconfigure.storage;

import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.RedisClient;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.spring.autoconfigure.JobRunrAutoConfiguration;
import org.jobrunr.spring.autoconfigure.JobRunrProperties;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.redis.LettuceRedisStorageProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
@ConditionalOnBean(LettuceConnectionFactory.class)
@AutoConfigureAfter(RedisAutoConfiguration.class)
@AutoConfigureBefore(JobRunrAutoConfiguration.class)
@ConditionalOnProperty(prefix = "org.jobrunr.database", name = "type", havingValue = "redis-lettuce", matchIfMissing = true)
public class JobRunrLettuceSpringDataConnectionFactoryStorageAutoConfiguration {

    @Bean(name = "storageProvider", destroyMethod = "close")
    @ConditionalOnMissingBean
    public StorageProvider lettuceSpringDataConnectionFactoryStorageProvider(LettuceConnectionFactory connectionFactory, JobMapper jobMapper, JobRunrProperties properties) {
        RedisClient redisClient = (RedisClient) connectionFactory.getNativeClient();
        LettuceRedisStorageProvider lettuceRedisStorageProvider = new LettuceRedisStorageProvider(redisClient, properties.getDatabase().getTablePrefix());
        lettuceRedisStorageProvider.setJobMapper(jobMapper);
        return lettuceRedisStorageProvider;
    }
}
