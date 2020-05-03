package org.jobrunr.utils;

import org.jobrunr.JobRunrException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClassPathUtils {

    private ClassPathUtils() {
    }

    private static Map<String, FileSystem> openFileSystems = new HashMap<>();

    public static Stream<Path> listAllChildrenOnClasspath(String... subFolder) {
        return listAllChildrenOnClasspath(ClassPathUtils.class, subFolder);
    }

    public static Stream<Path> listAllChildrenOnClasspath(Class clazz, String... subFolder) {
        try {
            return toPathsOnClasspath(clazz, subFolder)
                    .flatMap(ClassPathUtils::listAllChildrenOnClasspath);
        } catch (Exception e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }

    public static Stream<Path> toPathsOnClasspath(String... subFolder) {
        final List<Path> collect = toPathsOnClasspath(ClassPathUtils.class, subFolder).collect(Collectors.toList());
        return collect.stream();
    }

    public static Stream<Path> toPathsOnClasspath(Class clazz, String... subFolders) {
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
            if ("jar".equals(uri.getScheme())) {
                String jarName = uri.toString().substring(0, uri.toString().indexOf('!'));
                if (!openFileSystems.containsKey(jarName)) {
                    openFileSystems.put(jarName, FileSystems.newFileSystem(uri, Collections.emptyMap(), null));
                }
                return openFileSystems.get(jarName).getPath(uri.toString().substring(uri.toString().indexOf('!') + 1));
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
