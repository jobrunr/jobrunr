package org.jobrunr.storage.nosql.common.migrations;

import java.util.stream.Stream;

public interface NoSqlMigrationProvider {
    Stream<NoSqlMigration> getMigrations(Class<?> clazz);
}
