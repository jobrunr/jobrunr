package org.jobrunr.storage.nosql.redis;

import org.jobrunr.storage.nosql.common.migrations.NoSqlMigrationByClass;
import org.jobrunr.storage.nosql.redis.migrations.M001_JedisRemoveJobStatsAndUseMetadata;
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

    @Test
    public void testMigrationsHappyPath() {
        JedisRedisDBCreator jedisRedisDBCreator = new JedisRedisDBCreator(jedisRedisStorageProviderMock, getJedisPool(), "");

        assertThat(jedisRedisDBCreator.isNewMigration(new NoSqlMigrationByClass(M001_JedisRemoveJobStatsAndUseMetadata.class))).isTrue();

        assertThatCode(jedisRedisDBCreator::runMigrations).doesNotThrowAnyException();
        assertThatCode(jedisRedisDBCreator::runMigrations).doesNotThrowAnyException();

        assertThat(jedisRedisDBCreator.isNewMigration(new NoSqlMigrationByClass(M001_JedisRemoveJobStatsAndUseMetadata.class))).isTrue();
    }

    private JedisPool getJedisPool() {
        return new JedisPool(redisContainer.getContainerIpAddress(), redisContainer.getMappedPort(6379));
    }
}