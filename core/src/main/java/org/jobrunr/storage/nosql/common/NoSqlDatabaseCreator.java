package org.jobrunr.storage.nosql.common;

import org.jobrunr.JobRunrException;
import org.jobrunr.storage.nosql.NoSqlStorageProvider;
import org.jobrunr.storage.nosql.common.migrations.NoSqlMigration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

import static java.util.Comparator.comparing;

public abstract class NoSqlDatabaseCreator<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoSqlDatabaseCreator.class);

    private final NoSqlDatabaseMigrationsProvider databaseMigrationsProvider;

    protected NoSqlDatabaseCreator(NoSqlStorageProvider noSqlStorageProvider) {
        this(noSqlStorageProvider.getClass());
    }

    protected NoSqlDatabaseCreator(Class<? extends NoSqlStorageProvider> noSqlStorageProviderClass) {
        this.databaseMigrationsProvider = new NoSqlDatabaseMigrationsProvider(noSqlStorageProviderClass);
    }

    public void runMigrations() {
        getMigrations()
                .filter(this::isValidMigration)
                .sorted(comparing(NoSqlMigration::getClassName))
                .forEach(this::runMigrationIfNecessary);
    }

    protected boolean isValidMigration(NoSqlMigration noSqlMigration) {
        return true;
    }

    protected abstract boolean isNewMigration(NoSqlMigration noSqlMigration);

    protected abstract void runMigration(T noSqlMigration) throws Exception;

    protected abstract boolean markMigrationAsDone(NoSqlMigration noSqlMigration);

    protected void runMigrationIfNecessary(NoSqlMigration noSqlMigration) {
        if (!isNewMigration(noSqlMigration)) {
            LOGGER.info("Skipping migration {} as it is already done", noSqlMigration);
        } else {
            LOGGER.info("Running migration {}", noSqlMigration);
            try {
                runMigration((T) noSqlMigration.getMigration());
                markMigrationAsDone(noSqlMigration);
            } catch (Exception e) {
                throw JobRunrException.shouldNotHappenException(new IllegalStateException("Error running database migration " + noSqlMigration.getClassName(), e));
            }
        }
    }

    protected Stream<NoSqlMigration> getMigrations() {
        return databaseMigrationsProvider.getMigrations();
    }
}
