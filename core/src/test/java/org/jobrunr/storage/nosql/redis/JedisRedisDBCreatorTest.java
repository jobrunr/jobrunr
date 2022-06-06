package org.jobrunr.storage.nosql.redis;

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
import redis.clients.jedis.JedisPool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@Testcontainers
@ExtendWith(MockitoExtension.class)
class JedisRedisDBCreatorTest {

    @Container
    private static final GenericContainer redisContainer = new GenericContainer("redis").withExposedPorts(6379);

    @Mock
    private JedisRedisStorageProvider jedisRedisStorageProviderMock;

    private JedisPool jedisConnectionPool;
    private JedisRedisDBCreator jedisRedisDBCreator;

    @BeforeEach
    public void setupDBCreator() {
        jedisConnectionPool = getJedisPool();
        jedisRedisDBCreator = new JedisRedisDBCreator(jedisRedisStorageProviderMock, jedisConnectionPool, "");
    }

    @AfterEach
    public void teardownPool() {
        jedisConnectionPool.close();
    }

    @Test
    void testMigrationsHappyPath() {
        assertThat(jedisRedisDBCreator.isNewMigration(new NoSqlMigrationByClass(M001_JedisRemoveJobStatsAndUseMetadata.class))).isTrue();

        assertThatCode(jedisRedisDBCreator::runMigrations).doesNotThrowAnyException();
        assertThatCode(jedisRedisDBCreator::runMigrations).doesNotThrowAnyException();

        assertThat(jedisRedisDBCreator.isNewMigration(new NoSqlMigrationByClass(M001_JedisRemoveJobStatsAndUseMetadata.class))).isTrue();
    }

    private JedisPool getJedisPool() {
        return new JedisPool(redisContainer.getHost(), redisContainer.getMappedPort(6379));
    }
}