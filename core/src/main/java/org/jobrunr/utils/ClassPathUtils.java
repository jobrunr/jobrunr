package org.jobrunr.utils;

import org.jobrunr.JobRunrException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Enumeration;
import java.util.stream.Stream;

public class ClassPathUtils {

    private ClassPathUtils() {
    }

    public static Stream<Path> listAllChildrenOnClasspath(String... subFolder) {
        return listAllChildrenOnClasspath(ClassPathUtils.class, subFolder);
    }

    public static Stream<Path> listAllChildrenOnClasspath(Class<?> clazz, String... subFolder) {
        try {
            return toPathsOnClasspath(clazz, subFolder)
                    .flatMap(ClassPathUtils::listAllChildrenOnClasspath);
        } catch (Exception e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }

    public static Stream<Path> toPathsOnClasspath(String... subFolder) {
        return toPathsOnClasspath(ClassPathUtils.class, subFolder);
    }

    public static Stream<Path> toPathsOnClasspath(Class<?> clazz, String... subFolders) {
        return toPathsOnClasspath(clazz.getPackage(), subFolders);
    }

    public static Stream<Path> toPathsOnClasspath(Package pkg, String... subFolders) {
        final String joinedSubfolders = String.join("/", subFolders);
        if (joinedSubfolders.startsWith("/")) {
            return toUrls(joinedSubfolders.substring(1))
                    .map(ClassPathUtils::toPath);
        }
        return toUrls(pkg.getName().replace(".", "/") + "/" + joinedSubfolders)
                .map(ClassPathUtils::toPath);
    }

    private static Stream<URL> toUrls(String folder) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources(folder);

            return Collections.list(resources).stream();
        } catch (IOException e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }

    private static Path toPath(URL url) {
        try {
            URI uri = url.toURI();
            if ("wsjar".equals(uri.getScheme())) { // support for openliberty
                uri = new URI(uri.toString().replace("wsjar", "jar"));
            }
            if ("jar".equals(uri.getScheme())) {
                return JarFileSystemUtils.toPath(uri);
            } else {
                return Paths.get(uri);
            }
        } catch (IOException | URISyntaxException e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }

    private static Stream<Path> listAllChildrenOnClasspath(Path rootPath) {
        try {
            if (rootPath == null) return Stream.empty();
            if (Files.notExists(rootPath)) return Stream.empty();

            return Files.list(rootPath);
        } catch (IOException e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }
}
