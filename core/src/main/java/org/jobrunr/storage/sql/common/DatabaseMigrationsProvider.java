package org.jobrunr.storage.sql.common;

import org.jobrunr.storage.sql.SqlStorageProvider;
import org.jobrunr.storage.sql.common.migrations.DefaultSqlMigrationProvider;
import org.jobrunr.storage.sql.common.migrations.RunningOnJava11OrLowerWithinFatJarSqlMigrationProvider;
import org.jobrunr.storage.sql.common.migrations.SqlMigration;
import org.jobrunr.storage.sql.common.migrations.SqlMigrationProvider;
import org.jobrunr.utils.RuntimeUtils;
import org.jobrunr.utils.annotations.Because;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toMap;

@Because("https://github.com/jobrunr/jobrunr/issues/83")
class DatabaseMigrationsProvider {

    private final Class<? extends SqlStorageProvider> sqlStorageProviderClass;

    public DatabaseMigrationsProvider(Class<? extends SqlStorageProvider> sqlStorageProviderClass) {
        this.sqlStorageProviderClass = sqlStorageProviderClass;
    }

    public Stream<SqlMigration> getMigrations() {
        SqlMigrationProvider migrationProvider = getMigrationProvider();

        try {
            final Map<String, SqlMigration> commonMigrations = getCommonMigrations(migrationProvider).stream().collect(toMap(SqlMigration::getFileName, m -> m));
            final Map<String, SqlMigration> databaseSpecificMigrations = getDatabaseSpecificMigrations(migrationProvider).stream().collect(toMap(SqlMigration::getFileName, p -> p));

            final HashMap<String, SqlMigration> actualMigrations = new HashMap<>(commonMigrations);
            actualMigrations.putAll(databaseSpecificMigrations);

            return actualMigrations.values().stream();
        } catch (IllegalStateException e) {
            if(e.getMessage().startsWith("Duplicate key")) {
                throw new IllegalStateException("It seems you have JobRunr twice on your classpath. Please make sure to only have one JobRunr jar in your classpath.", e);
            }
            throw e;
        }
    }

    private SqlMigrationProvider getMigrationProvider() {
        if (RuntimeUtils.getJvmVersion() < 12 && RuntimeUtils.isRunningFromNestedJar()) {
            return new RunningOnJava11OrLowerWithinFatJarSqlMigrationProvider();
        } else {
            return new DefaultSqlMigrationProvider();
        }
    }

    protected List<SqlMigration> getCommonMigrations(SqlMigrationProvider migrationProvider) {
        return migrationProvider.getMigrations(DatabaseCreator.class);
    }

    protected List<SqlMigration> getDatabaseSpecificMigrations(SqlMigrationProvider migrationProvider) {
        if (sqlStorageProviderClass != null) {
            return migrationProvider.getMigrations(sqlStorageProviderClass);
        }
        return emptyList();
    }
}
