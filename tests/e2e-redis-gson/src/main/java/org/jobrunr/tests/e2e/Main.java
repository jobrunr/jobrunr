package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.redis.RedisStorageProvider;
import redis.clients.jedis.JedisPool;

public class Main extends AbstractMain {

    public static void main(String[] args) throws Exception {
        new Main(args);
    }

    public Main(String[] args) throws Exception {
        super(args);
    }

    @Override
    protected StorageProvider initStorageProvider() throws Exception {
        if (getEnvOrProperty("REDIS_HOST") == null) {
            throw new IllegalStateException("Cannot start BackgroundJobServer: environment variable REDIS_HOST is not set");
        }
        if (getEnvOrProperty("REDIS_PORT") == null) {
            throw new IllegalStateException("Cannot start BackgroundJobServer: environment variable MONREDIS_PORTGO_PORT is not set");
        }

        return new RedisStorageProvider(new JedisPool(getEnvOrProperty("REDIS_HOST"), Integer.parseInt(getEnvOrProperty("REDIS_PORT"))));
    }
}
