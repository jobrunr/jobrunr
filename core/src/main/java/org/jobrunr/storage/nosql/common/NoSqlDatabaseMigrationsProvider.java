package org.jobrunr.storage.nosql.common;

import org.jobrunr.storage.nosql.NoSqlStorageProvider;
import org.jobrunr.storage.nosql.common.migrations.DefaultNoSqlMigrationProvider;
import org.jobrunr.storage.nosql.common.migrations.NoSqlMigration;
import org.jobrunr.storage.nosql.common.migrations.NoSqlMigrationProvider;
import org.jobrunr.storage.nosql.common.migrations.RunningOnJava11OrLowerWithinFatJarNoSqlMigrationProvider;
import org.jobrunr.utils.RuntimeUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;

public class NoSqlDatabaseMigrationsProvider {

    private List<Class<? extends NoSqlStorageProvider>> noSqlStorageProviderClasses;

    public NoSqlDatabaseMigrationsProvider(List<Class<? extends NoSqlStorageProvider>> noSqlStorageProviderClasses) {
        this.noSqlStorageProviderClasses = noSqlStorageProviderClasses;
    }

    public Stream<NoSqlMigration> getMigrations() {
        NoSqlMigrationProvider migrationProvider = getMigrationProvider();
        return getMigrations(migrationProvider);
    }

    protected Stream<NoSqlMigration> getMigrations(NoSqlMigrationProvider migrationProvider) {
        Map<String, NoSqlMigration> migrations = new HashMap<>();
        for (Class<? extends NoSqlStorageProvider> noSqlStorageProvider : noSqlStorageProviderClasses) {
            migrationProvider
                    .getMigrations(noSqlStorageProvider)
                    .stream()
                    .filter(m -> m.getClassName().matches("^M[0-9]{3}_(.)*$"))
                    .forEach(m -> migrations.put(m.getClassName(), m));
        }
        return migrations.values().stream().sorted(comparing(NoSqlMigration::getClassName));
    }

    private NoSqlMigrationProvider getMigrationProvider() {
        if (RuntimeUtils.getJvmVersion() < 12 && RuntimeUtils.isRunningFromNestedJar()) {
            return new RunningOnJava11OrLowerWithinFatJarNoSqlMigrationProvider();
        } else {
            return new DefaultNoSqlMigrationProvider();
        }
    }
}
