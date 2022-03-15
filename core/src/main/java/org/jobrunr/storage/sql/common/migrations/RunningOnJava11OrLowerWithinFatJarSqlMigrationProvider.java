package org.jobrunr.storage.sql.common.migrations;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class RunningOnJava11OrLowerWithinFatJarSqlMigrationProvider implements SqlMigrationProvider {

    @Override
    public List<SqlMigration> getMigrations(Class<?> clazz) {
        try {
            URL location = clazz.getProtectionDomain().getCodeSource().getLocation();
            URLConnection urlConnection = location.openConnection();
            try(ZipInputStream zipInputStream = new ZipInputStream(urlConnection.getInputStream())) {
                return getMigrationsFromZipInputStream(zipInputStream, clazz);
            }
        } catch (IOException e) {
            throw new UnsupportedOperationException("Unable to find migrations.");
        }
    }

    private List<SqlMigration> getMigrationsFromZipInputStream(ZipInputStream zipInputStream, Class<?> clazz) throws IOException {
        List<SqlMigration> result = new ArrayList<>();
        ZipEntry zipEntry = zipInputStream.getNextEntry();
        while (zipEntry != null) {
            if (isSqlMigration(clazz, zipEntry)) {
                result.add(getSqlMigrationFromZipEntry(zipInputStream, zipEntry));
            }
            zipEntry = zipInputStream.getNextEntry();
        }
        return result;
    }

    private SqlMigrationByZipEntry getSqlMigrationFromZipEntry(ZipInputStream zipInputStream, ZipEntry zipEntry) throws IOException {
        StringBuilder s = new StringBuilder();
        int len;
        byte[] buffer = new byte[2048];
        while ((len = zipInputStream.read(buffer, 0, 1024)) >= 0) {
            s.append(new String(buffer, 0, len));
        }
        return new SqlMigrationByZipEntry(zipEntry.getName(), s.toString());
    }

    private boolean isSqlMigration(Class<?> clazz, ZipEntry zipEntry) {
        return zipEntry.getName().startsWith(clazz.getPackage().getName().replace(".", "/") + "/migrations") && zipEntry.getName().endsWith(".sql");
    }
}
