package org.jobrunr.micronaut.autoconfigure.storage;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.micronaut.context.ApplicationContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.redis.LettuceRedisStorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.jobrunr.micronaut.MicronautAssertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest(rebuildContext = true)
class JobRunrLettuceRedisStorageProviderFactoryTest {

    @Inject
    ApplicationContext context;

    @BeforeEach
    void setupRedisClient() {
        context.registerSingleton(redisClient());
    }

    @Test
    void lettuceRedisStorageProviderAutoConfigurationTest() {
        assertThat(context).hasSingleBean(StorageProvider.class);
        assertThat(context.getBean(StorageProvider.class))
                .isInstanceOf(LettuceRedisStorageProvider.class)
                .hasJobMapper();
        assertThat(context).doesNotHaveBean(InMemoryStorageProvider.class);
    }

    public RedisClient redisClient() {
        RedisClient redisClient = mock(RedisClient.class);
        StatefulRedisConnection connection = mock(StatefulRedisConnection.class);
        when(connection.sync()).thenReturn(mock(RedisCommands.class));
        when(redisClient.connect()).thenReturn(connection);
        return redisClient;
    }
}
