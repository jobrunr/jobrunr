package org.jobrunr.storage.sql.common.migrations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SqlMigrationByPath implements SqlMigration {

    private final Path path;
    private final String fileName;
    private final String migrationSql;

    public SqlMigrationByPath(Path path) {
        this.path = path;
        this.fileName = path.getFileName().toString();
        try {
            this.migrationSql = new String(Files.readAllBytes(path));
        } catch (IOException e) {
            throw new UnsupportedOperationException("Unable to read sql migration from file " + this.fileName, e);
        }
    }

    @Override
    public String getFileName() {
        return this.fileName;
    }

    @Override
    public String getMigrationSql() throws IOException {
        return migrationSql;
    }

    @Override
    public String toString() {
        return "MigrationByPath{" +
                "path=" + path +
                '}';
    }
}
