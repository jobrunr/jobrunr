package org.jobrunr.storage.sql.common;

import org.jetbrains.annotations.NotNull;
import org.jobrunr.storage.sql.SqlStorageProvider;
import org.jobrunr.storage.sql.common.migrations.DefaultSqlMigrationProvider;
import org.jobrunr.storage.sql.common.migrations.SqlMigration;
import org.jobrunr.storage.sql.mariadb.MariaDbStorageProviderStub;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
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
    void testDatabaseSpecificMigrationsShouldOverwriteSomeCommonMigrationFile() throws IOException, ClassNotFoundException {
        var provider = new DefaultSqlMigrationProvider();
        var commonMigrations = provider.getMigrations(DatabaseCreator.class).stream().map(SqlMigration::getFileName).collect(Collectors.toList());

        for (var providerPackageAndClass : findAllSqlStorageProvidersInFactorySource()) {
            var providerClass = Class.forName(SqlStorageProvider.class.getPackage().getName() + providerPackageAndClass);
            var specificMigrations = provider.getMigrations(providerClass).stream().map(SqlMigration::getFileName).collect(Collectors.toList());

            assertSpecificMigrationsOverwriteCommonOnes(specificMigrations, commonMigrations);
        }
    }

    private static @NotNull List<String> findAllSqlStorageProvidersInFactorySource() throws IOException {
        var srcLineToLookFor = "return getStorageProviderClass(SqlStorageProvider.class.getPackage().getName() + \"";
        var providerFactoryPath = Paths.get("src/main/java/" + SqlStorageProviderFactory.class.getPackageName().replaceAll("\\.", "/") + "/" + SqlStorageProviderFactory.class.getSimpleName() + ".java");

        return Files.readAllLines(providerFactoryPath)
                .stream().filter(line -> line.contains(srcLineToLookFor))
                .map(line -> line.trim().replace(srcLineToLookFor, "").replace("\");", ""))
                .collect(Collectors.toList());
    }

    private void assertSpecificMigrationsOverwriteCommonOnes(List<String> specificMigrations, List<String> commonMigrations) {
        assertThat(specificMigrations).isSubsetOf(commonMigrations);
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