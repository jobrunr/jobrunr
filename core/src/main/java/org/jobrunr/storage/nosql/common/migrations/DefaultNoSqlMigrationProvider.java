package org.jobrunr.storage.nosql.common.migrations;

import org.jobrunr.utils.resources.ClassPathResourceProvider;

import java.util.List;

import static java.util.stream.Collectors.toList;

public class DefaultNoSqlMigrationProvider implements NoSqlMigrationProvider {

    @Override
    public List<NoSqlMigration> getMigrations(Class<?> clazz) {
        try(ClassPathResourceProvider resourceProvider = new ClassPathResourceProvider()) {
            return resourceProvider.listAllChildrenOnClasspath(clazz, "migrations")
                    .map(NoSqlMigrationByPath::new)
                    .collect(toList());
        }
    }
}
