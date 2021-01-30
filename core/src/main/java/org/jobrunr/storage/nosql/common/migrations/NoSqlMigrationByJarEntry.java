package org.jobrunr.storage.nosql.common.migrations;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.jobrunr.utils.StringUtils.substringAfterLast;
import static org.jobrunr.utils.reflection.ReflectionUtils.toClassNameFromFileName;

public class NoSqlMigrationByJarEntry implements NoSqlMigration {

    private final JarFile jarFile;
    private final JarEntry jarEntry;
    private final URLClassLoader classLoader;

    public NoSqlMigrationByJarEntry(JarURLConnection jarURLConnection, JarFile jarFile, JarEntry jarEntry) {
        this.jarFile = jarFile;
        this.jarEntry = jarEntry;
        this.classLoader = URLClassLoader.newInstance(new URL[]{jarURLConnection.getJarFileURL()});
    }

    @Override
    public String getClassName() {
        return substringAfterLast(jarEntry.getName(), "/");
    }

    @Override
    public Class<?> getMigrationClass() throws IOException, ClassNotFoundException {
        String className = toClassNameFromFileName(jarEntry.getName());
        return classLoader.loadClass(className);
    }

    @Override
    public String toString() {
        return "MigrationByJarEntry{" +
                "jarFile=" + jarFile +
                ", jarEntry=" + jarEntry +
                '}';
    }
}
