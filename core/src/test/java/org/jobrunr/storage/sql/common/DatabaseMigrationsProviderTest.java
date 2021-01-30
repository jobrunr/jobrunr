package org.jobrunr.storage.sql.common;

import org.jobrunr.storage.sql.common.migrations.SqlMigration;
import org.jobrunr.storage.sql.mariadb.MariaDbStorageProviderStub;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseMigrationsProviderTest {

    @Test
    void testGetMigrations() {
        final DatabaseMigrationsProvider databaseCreator = new DatabaseMigrationsProvider(null);
        final Stream<SqlMigration> databaseSpecificMigrations = databaseCreator.getMigrations();
        assertThat(databaseSpecificMigrations).anyMatch(migration -> migration.getFileName().equals("v000__create_migrations_table.sql"));
    }

    @Test
    void testDatabaseSpecificMigrations() {
        final DatabaseMigrationsProvider databaseCreator = new DatabaseMigrationsProvider(MariaDbStorageProviderStub.class);
        final Stream<SqlMigration> databaseSpecificMigrations = databaseCreator.getMigrations();

        assertThat(databaseSpecificMigrations).anyMatch(migration -> contains(migration, "DATETIME(6)"));
    }

    private boolean contains(SqlMigration migration, String toContain) {
        try {
            return migration.getMigrationSql().contains(toContain);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}