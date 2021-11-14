package org.jobrunr.storage.nosql.redis;

import io.lettuce.core.api.StatefulRedisConnection;
import org.apache.commons.pool2.ObjectPool;
import org.jobrunr.storage.nosql.NoSqlStorageProvider;
import org.jobrunr.storage.nosql.common.NoSqlDatabaseCreator;
import org.jobrunr.storage.nosql.common.migrations.NoSqlMigration;
import org.jobrunr.storage.nosql.redis.migrations.LettuceRedisMigration;

public class LettuceRedisDBCreator extends NoSqlDatabaseCreator<LettuceRedisMigration> {

    private final ObjectPool<StatefulRedisConnection<String, String>> pool;
    private final String keyPrefix;

    public LettuceRedisDBCreator(NoSqlStorageProvider noSqlStorageProvider, ObjectPool<StatefulRedisConnection<String, String>> pool, String keyPrefix) {
        super(noSqlStorageProvider);
        this.pool = pool;
        this.keyPrefix = keyPrefix;
    }

    @Override
    protected boolean isValidMigration(NoSqlMigration noSqlMigration) {
        return noSqlMigration.getClassName().contains("Lettuce");
    }

    @Override
    protected boolean isNewMigration(NoSqlMigration noSqlMigration) {
        // why: as Jedis does not have a schema, each migration checks if it needs to do something
        return true;
    }

    @Override
    protected void runMigration(LettuceRedisMigration noSqlMigration) throws Exception {
        try (StatefulRedisConnection<String, String> connection = getConnection()) {
            noSqlMigration.runMigration(connection, keyPrefix);
        }
    }

    @Override
    protected boolean markMigrationAsDone(NoSqlMigration noSqlMigration) {
        return true;
    }

    protected StatefulRedisConnection<String, String> getConnection() {
        try {
            StatefulRedisConnection<String, String> statefulRedisConnection = pool.borrowObject();
            statefulRedisConnection.setAutoFlushCommands(true);
            return statefulRedisConnection;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
