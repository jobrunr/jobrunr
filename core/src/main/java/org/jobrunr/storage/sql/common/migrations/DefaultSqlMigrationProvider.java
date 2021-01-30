package org.jobrunr.storage.sql.common.migrations;

import java.util.stream.Stream;

import static org.jobrunr.utils.ClassPathUtils.listAllChildrenOnClasspath;

public class DefaultSqlMigrationProvider implements SqlMigrationProvider {

    @Override
    public Stream<SqlMigration> getMigrations(Class<?> clazz) {
        return listAllChildrenOnClasspath(clazz, "migrations")
                .filter(path -> path.toString().endsWith(".sql"))
                .map(SqlMigrationByPath::new);
    }
}
