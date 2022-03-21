package org.jobrunr.micronaut.autoconfigure.storage;

import io.lettuce.core.RedisClient;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.micronaut.autoconfigure.JobRunrConfiguration;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.redis.LettuceRedisStorageProvider;

@Factory
@Requires(classes = {RedisClient.class})
@Requires(beans = {RedisClient.class})
@Requires(property = "jobrunr.database.type", value = "redis-lettuce", defaultValue = "redis-lettuce")
public class JobRunrLettuceRedisStorageProviderFactory {

    @Inject
    private JobRunrConfiguration configuration;

    @Singleton
    @Primary
    public StorageProvider lettuceRedisStorageProvider(RedisClient redisClient, JobMapper jobMapper) {
        String tablePrefix = configuration.getDatabase().getTablePrefix().orElse(null);
        LettuceRedisStorageProvider lettuceRedisStorageProvider = new LettuceRedisStorageProvider(redisClient, tablePrefix);
        lettuceRedisStorageProvider.setJobMapper(jobMapper);
        return lettuceRedisStorageProvider;
    }
}
