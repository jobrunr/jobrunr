package org.jobrunr.storage.nosql.common.migrations;

import java.nio.file.Path;

import static org.jobrunr.utils.reflection.ReflectionUtils.toClassFromPath;

public class NoSqlMigrationByPath implements NoSqlMigration {

    private final Path path;
    private final String className;
    private final Class<?> migrationClass;

    public NoSqlMigrationByPath(Path path) {
        this.path = path;
        this.className = path.getFileName().toString();
        this.migrationClass = toClassFromPath(path);
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public Class<?> getMigrationClass() {
        return migrationClass;
    }

    @Override
    public String toString() {
        return "NoSqlMigrationByPath{" +
                "path=" + path +
                '}';
    }
}
