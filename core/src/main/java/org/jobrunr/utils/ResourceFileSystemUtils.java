package org.jobrunr.utils;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.ProviderNotFoundException;
import java.util.HashMap;

public class ResourceFileSystemUtils {

    private static FileSystem fileSystem;

    private ResourceFileSystemUtils() {
    }

    public static Path toPath(URI uri) throws IOException {
        try {
            if (!"resource".equals(uri.getScheme())) {
                throw new IllegalArgumentException("ResourceFileSystemUtils only supports uri's starting with resource:");
            }

            FileSystem fs = getFileSystem();
            return fs.getPath(StringUtils.substringAfter(uri.toString(), ":"));
        } catch (ProviderNotFoundException e) {
            throw new ProviderNotFoundException("Provider not found for URI " + uri.toString());
        }
    }

    private static FileSystem getFileSystem() throws IOException {
        if (fileSystem == null) {
            HashMap<String, Object> options = new HashMap<>();
            options.put("create", Boolean.TRUE);
            fileSystem = FileSystems.newFileSystem(URI.create("resource:/resources"), options, null);
        }
        return fileSystem;
    }
}
