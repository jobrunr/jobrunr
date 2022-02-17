package org.jobrunr.storage.nosql.common;

import org.jobrunr.storage.nosql.NoSqlStorageProvider;
import org.jobrunr.storage.nosql.common.migrations.DefaultNoSqlMigrationProvider;
import org.jobrunr.storage.nosql.common.migrations.NoSqlMigration;
import org.jobrunr.storage.nosql.common.migrations.NoSqlMigrationProvider;
import org.jobrunr.storage.nosql.common.migrations.RunningOnJava11OrLowerWithinFatJarNoSqlMigrationProvider;
import org.jobrunr.utils.RuntimeUtils;

import java.util.stream.Stream;

public class NoSqlDatabaseMigrationsProvider {

    private Class<? extends NoSqlStorageProvider> noSqlStorageProviderClass;

    public NoSqlDatabaseMigrationsProvider(Class<? extends NoSqlStorageProvider> noSqlStorageProviderClass) {
        this.noSqlStorageProviderClass = noSqlStorageProviderClass;
    }

    public Stream<NoSqlMigration> getMigrations() {
        NoSqlMigrationProvider migrationProvider = getMigrationProvider();
        return getMigrations(migrationProvider);
    }

    protected Stream<NoSqlMigration> getMigrations(NoSqlMigrationProvider migrationProvider) {
        return migrationProvider
                .getMigrations(noSqlStorageProviderClass)
                .stream()
                .filter(m -> m.getClassName().matches("^M[0-9]{3}_(.)*$"));
    }

    private NoSqlMigrationProvider getMigrationProvider() {
        if (RuntimeUtils.getJvmVersion() < 12 && RuntimeUtils.isRunningFromNestedJar()) {
            return new RunningOnJava11OrLowerWithinFatJarNoSqlMigrationProvider();
        } else {
            return new DefaultNoSqlMigrationProvider();
        }
    }
}
