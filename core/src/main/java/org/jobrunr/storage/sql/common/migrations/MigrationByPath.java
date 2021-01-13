package org.jobrunr.storage.sql.common.migrations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MigrationByPath implements Migration {

    private final Path path;

    public MigrationByPath(Path path) {
        this.path = path;
    }

    @Override
    public String getFileName() {
        return path.getFileName().toString();
    }

    @Override
    public String getMigrationSql() throws IOException {
        return new String(Files.readAllBytes(path));
    }

    @Override
    public String toString() {
        return "MigrationByPath{" +
                "path=" + path +
                '}';
    }
}
