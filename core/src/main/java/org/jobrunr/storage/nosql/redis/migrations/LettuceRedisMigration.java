package org.jobrunr.storage.nosql.redis.migrations;

import io.lettuce.core.api.StatefulRedisConnection;

import java.io.IOException;

public abstract class LettuceRedisMigration {

    public abstract void runMigration(StatefulRedisConnection<String, String> connection, String keyPrefix) throws IOException;
}
