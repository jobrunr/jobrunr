package org.jobrunr.storage.nosql.common;

import org.jobrunr.storage.nosql.common.migrations.NoSqlMigration;
import org.jobrunr.storage.nosql.documentdb.AmazonDocumentDBStorageProvider;
import org.jobrunr.storage.nosql.documentdb.migrations.M007_UpdateJobsCollectionReplaceIndices;
import org.jobrunr.storage.nosql.mongo.MongoDBStorageProvider;
import org.jobrunr.storage.nosql.mongo.migrations.M001_CreateJobCollection;
import org.jobrunr.storage.nosql.mongo.migrations.MongoMigration;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static org.assertj.core.api.Assertions.assertThat;

class NoSqlDatabaseMigrationsProviderTest {

    @Test
    void testNoSqlDatabaseMigrations() {
        final NoSqlDatabaseMigrationsProvider databaseCreator = new NoSqlDatabaseMigrationsProvider(singletonList(MongoDBStorageProvider.class));
        final Stream<NoSqlMigration> databaseSpecificMigrations = databaseCreator.getMigrations();

        assertThat(databaseSpecificMigrations).anyMatch(migration -> migration.getClassName().equals(M001_CreateJobCollection.class.getSimpleName() + ".class"));
    }

    @Test
    void testNoSqlDatabaseMigrationsAreSortedCorrectly() {
        final NoSqlDatabaseMigrationsProvider databaseCreator = new NoSqlDatabaseMigrationsProvider(asList(MongoDBStorageProvider.class, AmazonDocumentDBStorageProvider.class));
        final Stream<NoSqlMigration> databaseSpecificMigrations = databaseCreator.getMigrations();

        assertThat(databaseSpecificMigrations)
                .isSortedAccordingTo(comparing(NoSqlMigration::getClassName));
    }

    @Test
    void testNoSqlDatabaseMigrationsWhereMigrationsCanBeOverridden() {
        final NoSqlDatabaseMigrationsProvider databaseCreator = new NoSqlDatabaseMigrationsProvider(asList(MongoDBStorageProvider.class, AmazonDocumentDBStorageProvider.class));
        final Stream<NoSqlMigration> databaseSpecificMigrations = databaseCreator.getMigrations();

        assertThat(databaseSpecificMigrations)
                .anyMatch(migration -> matchesMigration(migration, M001_CreateJobCollection.class))
                .anyMatch(migration -> matchesMigration(migration, M007_UpdateJobsCollectionReplaceIndices.class))
                .anyMatch(migration -> {
                    try {
                        return migration.getMigrationClass().getName().contains("documentdb");
                    } catch (Exception e) {
                        return false;
                    }
                });
    }

    private boolean matchesMigration(NoSqlMigration migration, Class<? extends MongoMigration> migrationClass) {
        try {
            return migration.getMigrationClass().equals(migrationClass);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}