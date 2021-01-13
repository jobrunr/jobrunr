package org.jobrunr.storage.sql.common.migrations;

import java.util.stream.Stream;

public interface MigrationProvider {
    Stream<Migration> getMigrations(Class<?> clazz);
}
