package org.jobrunr.utils.resources;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathFileSystemProvider implements FileSystemProvider {
    @Override
    public Path toPath(URI uri) throws IOException {
        return Paths.get(uri);
    }

    @Override
    public void close() throws IOException {
        // nothing to do
    }
}
