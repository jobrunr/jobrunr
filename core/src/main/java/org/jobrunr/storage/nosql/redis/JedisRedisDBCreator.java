package org.jobrunr.storage.nosql.redis;

import org.jobrunr.storage.nosql.NoSqlStorageProvider;
import org.jobrunr.storage.nosql.common.NoSqlDatabaseCreator;
import org.jobrunr.storage.nosql.common.migrations.NoSqlMigration;
import org.jobrunr.storage.nosql.redis.migrations.JedisRedisMigration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class JedisRedisDBCreator extends NoSqlDatabaseCreator<JedisRedisMigration> {

    private final JedisPool jedisPool;
    private final String keyPrefix;

    public JedisRedisDBCreator(NoSqlStorageProvider noSqlStorageProvider, JedisPool jedisPool, String keyPrefix) {
        super(noSqlStorageProvider);
        this.jedisPool = jedisPool;
        this.keyPrefix = keyPrefix;
    }

    @Override
    protected boolean isValidMigration(NoSqlMigration noSqlMigration) {
        return noSqlMigration.getClassName().contains("Jedis");
    }

    @Override
    protected boolean isNewMigration(NoSqlMigration noSqlMigration) {
        // why: as Jedis does not have a schema, each migration checks if it needs to do something
        return true;
    }

    @Override
    protected void runMigration(JedisRedisMigration noSqlMigration) throws Exception {
        try (Jedis jedis = getJedis()) {
            noSqlMigration.runMigration(jedis, keyPrefix);
        }
    }

    @Override
    protected boolean markMigrationAsDone(NoSqlMigration noSqlMigration) {
        return true;
    }

    protected Jedis getJedis() {
        return jedisPool.getResource();
    }
}
