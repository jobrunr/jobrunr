package org.jobrunr.storage.nosql.redis.migrations;


import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import java.io.IOException;

import static java.lang.Long.parseLong;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;
import static org.jobrunr.storage.StorageProviderUtils.Metadata;
import static org.jobrunr.storage.nosql.redis.RedisUtilities.jobCounterKey;
import static org.jobrunr.storage.nosql.redis.RedisUtilities.metadataKey;
import static org.jobrunr.utils.StringUtils.isNotNullOrEmpty;

public class M001_LettuceRemoveJobStatsAndUseMetadata extends LettuceRedisMigration {

    @Override
    public void runMigration(StatefulRedisConnection<String, String> connection, String keyPrefix) throws IOException {
        RedisCommands<String, String> commands = connection.sync();

        if (commands.hget(metadataKey(keyPrefix, Metadata.STATS_ID), Metadata.FIELD_VALUE) != null) return;

        final String succeededCounterResponse = commands.get(jobCounterKey(SUCCEEDED)) != null ? commands.get(jobCounterKey(SUCCEEDED)) : null;
        if (isNotNullOrEmpty(succeededCounterResponse)) {
            commands.hincrby(metadataKey(keyPrefix, Metadata.STATS_ID), Metadata.FIELD_VALUE, parseLong(succeededCounterResponse));
            commands.del(jobCounterKey(SUCCEEDED));
        } else {
            commands.hset(metadataKey(keyPrefix, Metadata.STATS_ID), Metadata.FIELD_VALUE, "0");
        }
    }
}
