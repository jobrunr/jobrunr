package org.jobrunr.storage.sql.common.migrations;

import java.util.stream.Stream;

import static org.jobrunr.utils.ClassPathUtils.listAllChildrenOnClasspath;

public class DefaultMigrationProvider implements MigrationProvider {

    @Override
    public Stream<Migration> getMigrations(Class<?> clazz) {
        return listAllChildrenOnClasspath(clazz, "migrations")
                .filter(path -> path.toString().endsWith(".sql"))
                .map(MigrationByPath::new);
    }
}
