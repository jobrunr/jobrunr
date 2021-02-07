package org.jobrunr.storage.nosql.redis.migrations;

import redis.clients.jedis.Jedis;

import java.io.IOException;

import static java.lang.Long.parseLong;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;
import static org.jobrunr.storage.StorageProviderUtils.Metadata;
import static org.jobrunr.storage.nosql.redis.RedisUtilities.jobCounterKey;
import static org.jobrunr.storage.nosql.redis.RedisUtilities.metadataKey;
import static org.jobrunr.utils.StringUtils.isNotNullOrEmpty;

public class M001_JedisRemoveJobStatsAndUseMetadata extends JedisRedisMigration {

    @Override
    public void runMigration(Jedis jedis, String keyPrefix) throws IOException {
        if (jedis.hget(metadataKey(keyPrefix, Metadata.STATS_ID), Metadata.FIELD_VALUE) != null) return;

        final String succeededCounterResponse = jedis.get(jobCounterKey(SUCCEEDED));
        if (isNotNullOrEmpty(succeededCounterResponse)) {
            jedis.hincrBy(metadataKey(keyPrefix, Metadata.STATS_ID), Metadata.FIELD_VALUE, parseLong(succeededCounterResponse));
            jedis.del(jobCounterKey(SUCCEEDED));
        } else {
            jedis.hset(metadataKey(keyPrefix, Metadata.STATS_ID), Metadata.FIELD_VALUE, "0");
        }
    }
}
