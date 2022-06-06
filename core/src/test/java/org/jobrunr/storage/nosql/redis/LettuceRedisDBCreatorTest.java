package org.jobrunr.storage.nosql.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.support.ConnectionPoolSupport;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.jobrunr.storage.nosql.common.migrations.NoSqlMigrationByClass;
import org.jobrunr.storage.nosql.redis.migrations.M001_JedisRemoveJobStatsAndUseMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@Testcontainers
@ExtendWith(MockitoExtension.class)
class LettuceRedisDBCreatorTest {

    @Container
    private static final GenericContainer redisContainer = new GenericContainer("redis").withExposedPorts(6379);

    @Mock
    private LettuceRedisStorageProvider lettuceRedisStorageProviderMock;
    private GenericObjectPool<StatefulRedisConnection<String, String>> redisConnectionPool;
    private LettuceRedisDBCreator lettuceRedisDBCreator;

    @BeforeEach
    public void setupDBCreator() {
        redisConnectionPool = redisConnectionPool();
        lettuceRedisDBCreator = new LettuceRedisDBCreator(lettuceRedisStorageProviderMock, redisConnectionPool, "");
    }

    @AfterEach
    public void teardownPool() {
        redisConnectionPool.close();
    }

    @Test
    void testMigrationsHappyPath() {
        assertThat(lettuceRedisDBCreator.isNewMigration(new NoSqlMigrationByClass(M001_JedisRemoveJobStatsAndUseMetadata.class))).isTrue();

        assertThatCode(lettuceRedisDBCreator::runMigrations).doesNotThrowAnyException();
        assertThatCode(lettuceRedisDBCreator::runMigrations).doesNotThrowAnyException();

        assertThat(lettuceRedisDBCreator.isNewMigration(new NoSqlMigrationByClass(M001_JedisRemoveJobStatsAndUseMetadata.class))).isTrue();

    }

    private GenericObjectPool<StatefulRedisConnection<String, String>> redisConnectionPool() {
        return ConnectionPoolSupport.createGenericObjectPool(() -> createConnection(getRedisClient()), new GenericObjectPoolConfig());
    }

    private RedisClient getRedisClient() {
        return RedisClient.create(RedisURI.create(redisContainer.getHost(), redisContainer.getMappedPort(6379)));
    }

    StatefulRedisConnection<String, String> createConnection(RedisClient redisClient) {
        return redisClient.connect();
    }
}