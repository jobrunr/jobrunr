package org.jobrunr.utils.resources;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.jobrunr.utils.StringUtils.substringAfterLast;
import static org.jobrunr.utils.StringUtils.substringBeforeLast;

public class JarFileSystemProvider implements FileSystemProvider {

    private final Map<String, FileSystem> openFileSystems = new HashMap<>();

    public Path toPath(URI uri) throws IOException {
        try {
            if (!"jar".equals(uri.getScheme())) {
                throw new IllegalArgumentException("JarFileSystemProvider only supports uri's starting with jar:");
            }

            String fileSystemName = substringBeforeLast(uri.toString(), "!");
            String path = substringAfterLast(uri.toString(), "!");

            FileSystem fs = getFileSystem(fileSystemName);
            return fs.getPath(path);
        } catch (ProviderNotFoundException e) {
            throw new ProviderNotFoundException("Provider not found for URI " + uri.toString());
        }
    }

    private FileSystem getFileSystem(String fileSystemName) throws IOException {
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
                openFileSystems.put(fileSystemName, FileSystems.newFileSystem(path, ClassLoader.getSystemClassLoader()));
            }
        }
        return openFileSystems.get(fileSystemName);
    }

    @Override
    public void close() throws IOException {
        // Nested filesystems need to be closed in order
        List<String> fileSystemNames = new ArrayList<>(openFileSystems.keySet());

        for(int i = 3; i >= 0; i--) {
            int finalI = i;
            List<String> fileSystemsToClose = fileSystemNames.stream()
                    .filter(fsName -> fsName.chars().filter(ch -> ch == '!').count() == finalI)
                    .collect(Collectors.toList());

            fileSystemsToClose.forEach(this::close);
            fileSystemsToClose.forEach(openFileSystems::remove);
        }
    }

    private void close(String fileSystemName) {
        try {
            openFileSystems.remove(fileSystemName).close();
        } catch (IOException e ){
            // nothing more we can do
        }
    }
}
