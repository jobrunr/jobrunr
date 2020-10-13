package org.jobrunr.autoconfigure.storage;

import io.lettuce.core.RedisClient;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.redis.LettuceRedisStorageProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnBean(RedisClient.class)
public class JobRunrLettuceStorageAutoConfiguration {

    @Bean(name = "storageProvider")
    @ConditionalOnMissingBean
    public StorageProvider jedisStorageProvider(RedisClient redisClient) {
        return new LettuceRedisStorageProvider(redisClient);
    }
}
