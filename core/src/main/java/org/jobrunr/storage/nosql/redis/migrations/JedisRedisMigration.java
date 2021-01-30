package org.jobrunr.storage.nosql.redis.migrations;

import redis.clients.jedis.Jedis;

import java.io.IOException;

public abstract class JedisRedisMigration {

    public abstract void runMigration(Jedis jedis, String keyPrefix) throws IOException;
}
