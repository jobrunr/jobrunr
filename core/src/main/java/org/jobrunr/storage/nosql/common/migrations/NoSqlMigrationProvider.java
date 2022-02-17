package org.jobrunr.storage.nosql.common.migrations;

import java.util.List;

public interface NoSqlMigrationProvider {
    List<NoSqlMigration> getMigrations(Class<?> clazz);
}
