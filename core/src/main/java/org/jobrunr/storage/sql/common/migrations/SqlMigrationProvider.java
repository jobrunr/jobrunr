package org.jobrunr.storage.sql.common.migrations;

import java.util.stream.Stream;

public interface SqlMigrationProvider {
    Stream<SqlMigration> getMigrations(Class<?> clazz);
}
