package org.jobrunr.storage.nosql.common.migrations;

import java.io.IOException;

public class NoSqlMigrationByClass implements NoSqlMigration {

    private Class<?> clazz;

    public NoSqlMigrationByClass(Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    public String getClassName() {
        return clazz.getSimpleName() + ".class";
    }

    @Override
    public Class<?> getMigrationClass() throws IOException, ClassNotFoundException {
        return clazz;
    }
}
