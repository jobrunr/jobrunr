package org.jobrunr.utils;

import org.jobrunr.JobRunrException;

import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;

public class PathUtils {

    private PathUtils() {
    }

    private static Map<String, FileSystem> openFileSystems = new HashMap<>();

    public static Stream<Path> listItems(Class clazz, String... subFolder) {
        return listItems(getResourcesPath(clazz, subFolder));
    }

    public static Stream<Path> listItems(Path rootPath) {
        try {
            if (rootPath == null) return Stream.empty();

            return Files.list(rootPath);
        } catch (Exception e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }

    public static Path getResourcesPath(Class clazz, String... subFolder) {
        return getResourcesPath("/" + clazz.getPackage().getName().replace(".", "/") + "/" + stream(subFolder).collect(Collectors.joining("/")));
    }

    public static Path getResourcesPath(String folder) {
        try {
            final URL resource = PathUtils.class.getResource(folder);
            if (resource == null) return null;

            URI uri = resource.toURI();
            if ("jar".equals(uri.getScheme())) {
                String jarName = uri.toString().substring(0, uri.toString().indexOf('!'));
                if (!openFileSystems.containsKey(jarName)) {
                    openFileSystems.put(jarName, FileSystems.newFileSystem(uri, Collections.emptyMap(), null));
                }
                return openFileSystems.get(jarName).getPath(folder);
            } else {
                return Paths.get(uri);
            }
        } catch (Exception e) {
            throw JobRunrException.shouldNotHappenException(new IllegalStateException("Error getting folder " + folder, e));
        }
    }
}
