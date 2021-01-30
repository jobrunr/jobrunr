package org.jobrunr.storage.nosql.common.migrations;

import java.util.stream.Stream;

import static org.jobrunr.utils.ClassPathUtils.listAllChildrenOnClasspath;

public class DefaultNoSqlMigrationProvider implements NoSqlMigrationProvider {

    @Override
    public Stream<NoSqlMigration> getMigrations(Class<?> clazz) {
        return listAllChildrenOnClasspath(clazz, "migrations")
                .map(NoSqlMigrationByPath::new);
    }
}
