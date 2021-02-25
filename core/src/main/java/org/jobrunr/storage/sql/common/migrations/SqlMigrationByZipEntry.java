package org.jobrunr.storage.sql.common.migrations;

import static org.jobrunr.utils.StringUtils.substringAfterLast;

public class SqlMigrationByZipEntry implements SqlMigration {

    private final String name;
    private final String contents;

    public SqlMigrationByZipEntry(String name, String contents) {
        this.name = name;
        this.contents = contents;
    }

    @Override
    public String getFileName() {
        return substringAfterLast(name, "/");
    }

    @Override
    public String getMigrationSql() {
        return contents;
    }

    @Override
    public String toString() {
        return "SqlMigrationByZipEntry{" +
                "name=" + name +
                '}';
    }
}
