package org.jobrunr.storage.nosql.common.migrations;

import org.jobrunr.utils.annotations.VisibleFor;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class RunningOnJava11OrLowerWithinFatJarNoSqlMigrationProvider implements NoSqlMigrationProvider {

    @Override
    public List<NoSqlMigration> getMigrations(Class<?> clazz) {
        try {
            URL location = clazz.getProtectionDomain().getCodeSource().getLocation();
            URLConnection urlConnection = location.openConnection();
            try(ZipInputStream zipInputStream = new ZipInputStream(urlConnection.getInputStream())) {
                return getMigrationsFromZipInputStream(zipInputStream, clazz);
            }
        } catch (IOException e) {
            throw new UnsupportedOperationException("Unable to find migrations.", e);
        } catch (ClassNotFoundException e) {
            throw new UnsupportedOperationException("Unable to load migrations from classpath.", e);
        }
    }

    private List<NoSqlMigration> getMigrationsFromZipInputStream(ZipInputStream zipInputStream, Class<?> clazz) throws IOException, ClassNotFoundException {
        List<NoSqlMigration> result = new ArrayList<>();
        ZipEntry zipEntry = zipInputStream.getNextEntry();
        while (zipEntry != null) {
            if (isNoSqlMigration(clazz, zipEntry)) {
                result.add(new NoSqlMigrationByZipEntry(zipEntry.getName()));
            }
            zipEntry = zipInputStream.getNextEntry();
        }
        return result;
    }

    @VisibleFor("testing - Github Issue #416")
    boolean isNoSqlMigration(Class<?> clazz, ZipEntry zipEntry) {
        return zipEntry.getName().startsWith(clazz.getPackage().getName().replace(".", "/") + "/migrations")
                && zipEntry.getName().endsWith(".class");
    }
}
