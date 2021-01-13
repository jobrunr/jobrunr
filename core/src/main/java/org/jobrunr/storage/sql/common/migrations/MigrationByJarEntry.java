package org.jobrunr.storage.sql.common.migrations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static java.util.stream.Collectors.joining;
import static org.jobrunr.utils.StringUtils.substringAfterLast;

public class MigrationByJarEntry implements Migration {

    private final JarFile jarFile;
    private final JarEntry jarEntry;

    public MigrationByJarEntry(JarFile jarFile, JarEntry jarEntry) {
        this.jarFile = jarFile;
        this.jarEntry = jarEntry;
    }

    @Override
    public String getFileName() {
        return substringAfterLast(jarEntry.getName(), "/");
    }

    @Override
    public String getMigrationSql() throws IOException {
        try (InputStream inputStream = jarFile.getInputStream(jarEntry)) {
            return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .lines()
                    .collect(joining("\n"));
        }
    }

    @Override
    public String toString() {
        return "MigrationByJarEntry{" +
                "jarFile=" + jarFile +
                ", jarEntry=" + jarEntry +
                '}';
    }
}
