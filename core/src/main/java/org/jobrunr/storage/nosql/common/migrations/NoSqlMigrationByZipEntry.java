package org.jobrunr.storage.nosql.common.migrations;

import java.io.IOException;

import static org.jobrunr.utils.StringUtils.substringAfterLast;
import static org.jobrunr.utils.reflection.ReflectionUtils.loadClass;
import static org.jobrunr.utils.reflection.ReflectionUtils.toClassNameFromFileName;

public class NoSqlMigrationByZipEntry implements NoSqlMigration {

    private final String name;
    private final String className;
    private final Class<?> migrationClass;

    public NoSqlMigrationByZipEntry(String name) throws ClassNotFoundException {
        this.name = name;
        this.className = substringAfterLast(name, "/");
        this.migrationClass = loadClass(toClassNameFromFileName(name));
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public Class<?> getMigrationClass() throws IOException, ClassNotFoundException {
        return migrationClass;
    }

    @Override
    public String toString() {
        return "NoSqlMigrationByZipEntry{" +
                "zipFile=" + name +
                '}';
    }
}
