package org.jobrunr.storage.nosql.common.migrations;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public class RunningOnJava11OrLowerWithinFatJarNoSqlMigrationProvider implements NoSqlMigrationProvider {

    @Override
    public Stream<NoSqlMigration> getMigrations(Class<?> clazz) {
        try {
            URL location = clazz.getProtectionDomain().getCodeSource().getLocation();
            URLConnection urlConnection = location.openConnection();
            if (urlConnection instanceof JarURLConnection) {
                return getMigrationsFromJarUrlConnection((JarURLConnection) urlConnection, clazz);
            }
        } catch (IOException e) {
        }
        throw new UnsupportedOperationException("Unable to find migrations.");
    }

    private Stream<NoSqlMigration> getMigrationsFromJarUrlConnection(JarURLConnection jarURLConnection, Class<?> clazz) throws IOException {
        JarFile jarFile = jarURLConnection.getJarFile();
        Predicate<JarEntry> jarEntryPredicate = jarEntry -> jarEntry.getName().startsWith(clazz.getPackage().getName().replace(".", "/") + "/migrations");
        return jarFile.stream()
                .filter(jarEntryPredicate)
                .map(jarEntry -> new NoSqlMigrationByJarEntry(jarURLConnection, jarFile, jarEntry));
    }
}
