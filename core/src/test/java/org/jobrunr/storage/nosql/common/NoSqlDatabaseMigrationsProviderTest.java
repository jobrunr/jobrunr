package org.jobrunr.storage.nosql.common;

import org.jobrunr.storage.nosql.common.migrations.NoSqlMigration;
import org.jobrunr.storage.nosql.mongo.MongoDBStorageProvider;
import org.jobrunr.storage.nosql.mongo.migrations.M001_CreateJobCollection;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class NoSqlDatabaseMigrationsProviderTest {

    @Test
    void testNoSqlDatabaseMigrations() {
        final NoSqlDatabaseMigrationsProvider databaseCreator = new NoSqlDatabaseMigrationsProvider(MongoDBStorageProvider.class);
        final Stream<NoSqlMigration> databaseSpecificMigrations = databaseCreator.getMigrations();

        assertThat(databaseSpecificMigrations).anyMatch(migration -> migration.getClassName().equals(M001_CreateJobCollection.class.getSimpleName() + ".class"));
    }
}