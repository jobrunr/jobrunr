package org.jobrunr.storage.nosql.common.migrations;

import org.jobrunr.utils.reflection.ReflectionUtils;

import java.io.IOException;
import java.nio.file.Path;

public class NoSqlMigrationByPath implements NoSqlMigration {

    private final Path path;

    public NoSqlMigrationByPath(Path path) {
        this.path = path;
    }

    @Override
    public String getClassName() {
        return path.getFileName().toString();
    }

    @Override
    public Class<?> getMigrationClass() throws IOException, ClassNotFoundException {
        return ReflectionUtils.toClassFromPath(path);
    }

    @Override
    public String toString() {
        return "NoSqlMigrationByPath{" +
                "path=" + path +
                '}';
    }
}
