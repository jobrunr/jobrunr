package org.jobrunr.storage.sql.common.migrations;

import java.util.List;

public interface SqlMigrationProvider {
    List<SqlMigration> getMigrations(Class<?> clazz);
}
