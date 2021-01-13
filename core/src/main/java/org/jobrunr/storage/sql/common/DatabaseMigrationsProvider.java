package org.jobrunr.storage.sql.common;

import org.jobrunr.storage.sql.SqlStorageProvider;
import org.jobrunr.storage.sql.common.migrations.DefaultMigrationProvider;
import org.jobrunr.storage.sql.common.migrations.Migration;
import org.jobrunr.storage.sql.common.migrations.MigrationProvider;
import org.jobrunr.storage.sql.common.migrations.RunningOnJava11OrLowerWithinFatJarMigrationProvider;
import org.jobrunr.utils.RuntimeUtils;
import org.jobrunr.utils.annotations.Because;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

@Because("https://github.com/jobrunr/jobrunr/issues/83")
class DatabaseMigrationsProvider {

    private final Class<? extends SqlStorageProvider> sqlStorageProviderClass;

    public DatabaseMigrationsProvider(Class<? extends SqlStorageProvider> sqlStorageProviderClass) {
        this.sqlStorageProviderClass = sqlStorageProviderClass;
    }

    public Stream<Migration> getMigrations() {
        MigrationProvider migrationProvider = getMigrationProvider();

        final Map<String, Migration> commonMigrations = getCommonMigrations(migrationProvider).collect(toMap(Migration::getFileName, m -> m));
        final Map<String, Migration> databaseSpecificMigrations = getDatabaseSpecificMigrations(migrationProvider).collect(toMap(Migration::getFileName, p -> p));

        final HashMap<String, Migration> actualMigrations = new HashMap<>(commonMigrations);
        actualMigrations.putAll(databaseSpecificMigrations);

        return actualMigrations.values().stream();
    }

    private MigrationProvider getMigrationProvider() {
        if (RuntimeUtils.getJvmVersion() < 12 && RuntimeUtils.isRunningFromNestedJar()) {
            return new RunningOnJava11OrLowerWithinFatJarMigrationProvider();
        } else {
            return new DefaultMigrationProvider();
        }
    }

    protected Stream<Migration> getCommonMigrations(MigrationProvider migrationProvider) {
        return migrationProvider.getMigrations(DatabaseCreator.class);
    }

    protected Stream<Migration> getDatabaseSpecificMigrations(MigrationProvider migrationProvider) {
        if (sqlStorageProviderClass != null) {
            return migrationProvider.getMigrations(sqlStorageProviderClass);
        }
        return Stream.empty();
    }
}
