package org.jobrunr.utils.resources;

import org.jobrunr.utils.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.ProviderNotFoundException;
import java.util.HashMap;
import java.util.Map;

public class ResourcesFileSystemProvider implements FileSystemProvider {

    private FileSystem fileSystem;

    public Path toPath(URI uri) throws IOException {
        try {
            if (!"resource".equals(uri.getScheme())) {
                throw new IllegalArgumentException("ResourcesFileSystemProvider only supports uri's starting with resource:");
            }

            FileSystem fs = getFileSystem();
            return fs.getPath(StringUtils.substringAfter(uri.toString(), ":"));
        } catch (ProviderNotFoundException e) {
            throw new ProviderNotFoundException("Provider not found for URI " + uri);
        }
    }

    private FileSystem getFileSystem() throws IOException {
        if (fileSystem == null) {
            Map<String, Object> options = new HashMap<>();
            options.put("create", Boolean.TRUE);
            fileSystem = FileSystems.newFileSystem(URI.create("resource:/resources"), options, null);
        }
        return fileSystem;
    }

    @Override
    public void close() throws IOException {
        fileSystem.close();
    }
}
