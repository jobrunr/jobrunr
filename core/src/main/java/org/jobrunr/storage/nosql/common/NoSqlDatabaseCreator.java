package org.jobrunr.storage.nosql.common;

import org.jobrunr.storage.nosql.NoSqlStorageProvider;
import org.jobrunr.storage.nosql.common.migrations.NoSqlMigration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static java.lang.Thread.sleep;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static org.jobrunr.JobRunrException.shouldNotHappenException;

public abstract class NoSqlDatabaseCreator<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoSqlDatabaseCreator.class);

    private static Random randomTimeOnFailure = new Random();

    private final NoSqlDatabaseMigrationsProvider databaseMigrationsProvider;

    protected NoSqlDatabaseCreator(NoSqlStorageProvider noSqlStorageProvider) {
        this(noSqlStorageProvider.getClass());
    }

    protected NoSqlDatabaseCreator(Class<? extends NoSqlStorageProvider> noSqlStorageProviderClass) {
        this(singletonList(noSqlStorageProviderClass));
    }

    protected NoSqlDatabaseCreator(List<Class<? extends NoSqlStorageProvider>> noSqlStorageProviderClasses) {
        this.databaseMigrationsProvider = new NoSqlDatabaseMigrationsProvider(noSqlStorageProviderClasses);
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
                runMigration(noSqlMigration.getMigration());
                markMigrationAsDone(noSqlMigration);
            } catch (Exception exceptionCausedByConcurrentMigration) {
                try {
                    sleep(randomTimeOnFailure.nextInt(1000));
                    if(isNewMigration(noSqlMigration)) {
                        runMigration(noSqlMigration.getMigration());
                        markMigrationAsDone(noSqlMigration);
                    }
                } catch(Exception e) {
                    throw shouldNotHappenException(new IllegalStateException("Error running database migration " + noSqlMigration.getClassName(), e));
                }
            }
        }
    }

    protected Stream<NoSqlMigration> getMigrations() {
        return databaseMigrationsProvider.getMigrations();
    }
}
