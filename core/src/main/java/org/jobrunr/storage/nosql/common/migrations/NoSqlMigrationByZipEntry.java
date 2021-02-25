package org.jobrunr.storage.nosql.common.migrations;

import java.io.IOException;

import static org.jobrunr.utils.StringUtils.substringAfterLast;
import static org.jobrunr.utils.reflection.ReflectionUtils.loadClass;
import static org.jobrunr.utils.reflection.ReflectionUtils.toClassNameFromFileName;

public class NoSqlMigrationByZipEntry implements NoSqlMigration {

    private final String name;

    public NoSqlMigrationByZipEntry(String name) {
        this.name = name;
    }

    @Override
    public String getClassName() {
        return substringAfterLast(name, "/");
    }

    @Override
    public Class<?> getMigrationClass() throws IOException, ClassNotFoundException {
        String className = toClassNameFromFileName(name);
        return loadClass(className);
    }

    @Override
    public String toString() {
        return "NoSqlMigrationByZipEntry{" +
                "zipFile=" + name +
                '}';
    }
}
