package org.jobrunr.storage.sql.common;

import org.jobrunr.storage.sql.SqlStorageProvider;
import org.jobrunr.storage.sql.common.migrations.DefaultSqlMigrationProvider;
import org.jobrunr.storage.sql.common.migrations.RunningOnJava11OrLowerWithinFatJarSqlMigrationProvider;
import org.jobrunr.storage.sql.common.migrations.SqlMigration;
import org.jobrunr.storage.sql.common.migrations.SqlMigrationProvider;
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

    public Stream<SqlMigration> getMigrations() {
        SqlMigrationProvider migrationProvider = getMigrationProvider();

        final Map<String, SqlMigration> commonMigrations = getCommonMigrations(migrationProvider).collect(toMap(SqlMigration::getFileName, m -> m));
        final Map<String, SqlMigration> databaseSpecificMigrations = getDatabaseSpecificMigrations(migrationProvider).collect(toMap(SqlMigration::getFileName, p -> p));

        final HashMap<String, SqlMigration> actualMigrations = new HashMap<>(commonMigrations);
        actualMigrations.putAll(databaseSpecificMigrations);

        return actualMigrations.values().stream();
    }

    private SqlMigrationProvider getMigrationProvider() {
        if (RuntimeUtils.getJvmVersion() < 12 && RuntimeUtils.isRunningFromNestedJar()) {
            return new RunningOnJava11OrLowerWithinFatJarSqlMigrationProvider();
        } else {
            return new DefaultSqlMigrationProvider();
        }
    }

    protected Stream<SqlMigration> getCommonMigrations(SqlMigrationProvider migrationProvider) {
        return migrationProvider.getMigrations(DatabaseCreator.class);
    }

    protected Stream<SqlMigration> getDatabaseSpecificMigrations(SqlMigrationProvider migrationProvider) {
        if (sqlStorageProviderClass != null) {
            return migrationProvider.getMigrations(sqlStorageProviderClass);
        }
        return Stream.empty();
    }
}
