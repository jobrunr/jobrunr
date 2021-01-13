package org.jobrunr.utils;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.jobrunr.utils.StringUtils.substringAfterLast;
import static org.jobrunr.utils.StringUtils.substringBeforeLast;

public class JarFileSystemUtils {

    private static final Map<String, FileSystem> openFileSystems = new HashMap<>();

    private JarFileSystemUtils() {

    }

    public static Path toPath(URI uri) throws IOException {
        if (!"jar".equals(uri.getScheme()))
            throw new IllegalArgumentException("JarFileSystemUtils only supports uri's starting with jar:");

        String path = substringAfterLast(uri.toString(), "!");
        String fileSystemName = substringBeforeLast(uri.toString(), "!");

        FileSystem fs = getFileSystem(fileSystemName);
        return fs.getPath(path);
    }

    private static FileSystem getFileSystem(String fileSystemName) throws IOException {
        if (!openFileSystems.containsKey(fileSystemName)) {
            if (!fileSystemName.contains("!")) {
                try {
                    openFileSystems.put(fileSystemName, FileSystems.newFileSystem(URI.create(fileSystemName), Collections.emptyMap(), null));
                } catch (FileSystemAlreadyExistsException e) {
                    openFileSystems.put(fileSystemName, FileSystems.getFileSystem(URI.create(fileSystemName)));
                }
            } else {
                FileSystem parent = getFileSystem(substringBeforeLast(fileSystemName, "!"));
                Path path = parent.getPath(substringAfterLast(fileSystemName, "!"));
                openFileSystems.put(fileSystemName, FileSystems.newFileSystem(path, null));
            }
        }
        return openFileSystems.get(fileSystemName);
    }
}
