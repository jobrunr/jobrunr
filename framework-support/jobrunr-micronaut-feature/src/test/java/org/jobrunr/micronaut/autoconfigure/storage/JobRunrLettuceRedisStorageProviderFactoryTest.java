package org.jobrunr.micronaut.autoconfigure.storage;

import io.lettuce.core.RedisClient;
import io.micronaut.context.ApplicationContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.redis.LettuceRedisStorageProvider;
import org.junit.jupiter.api.Test;

import static org.jobrunr.micronaut.MicronautAssertions.assertThat;

@MicronautTest
class JobRunrLettuceRedisStorageProviderFactoryTest {

    @Inject
    ApplicationContext context;

    @Test
    void lettuceRedisStorageProviderAutoConfigurationTest() {
        assertThat(context).hasSingleBean(StorageProvider.class);
        assertThat(context.getBean(StorageProvider.class))
                .isInstanceOf(LettuceRedisStorageProvider.class)
                .hasJobMapper();
        assertThat(context).doesNotHaveBean(InMemoryStorageProvider.class);
    }

    @Singleton
    public RedisClient redisClient() {
        return Mocks.redisClient();
    }
}
