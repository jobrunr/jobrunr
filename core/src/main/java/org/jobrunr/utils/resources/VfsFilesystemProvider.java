package org.jobrunr.utils.resources;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class VfsFilesystemProvider extends PathFileSystemProvider {

    public static final String SCHEME = "vfs://";
    private final Set<Path> extractedFiles;

    public VfsFilesystemProvider() {
        this.extractedFiles = new HashSet<>();
    }

    @Override
    public Path toPath(URI uri) throws IOException {
        uri = URI.create(uri.toString().substring(SCHEME.length()));
        Path path =  super.toPath(uri);
        extractedFiles.add(path);
        return path;
    }

    @Override
    public void close() throws IOException {
        for (Path path : extractedFiles) {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        super.close();
    }
}
