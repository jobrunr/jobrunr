package org.jobrunr.storage.sql.common.migrations;

import org.jobrunr.utils.resources.ClassPathResourceProvider;

import java.util.List;

import static java.util.stream.Collectors.toList;

public class DefaultSqlMigrationProvider implements SqlMigrationProvider {

    @Override
    public List<SqlMigration> getMigrations(Class<?> clazz) {
        try(ClassPathResourceProvider resourceProvider = new ClassPathResourceProvider()) {
            return resourceProvider.listAllChildrenOnClasspath(clazz, "migrations")
                    .filter(path -> path.toString().endsWith(".sql"))
                    .map(SqlMigrationByPath::new)
                    .collect(toList());
        }
    }
}
