package org.jobrunr.utils.resources;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

public interface FileSystemProvider extends Closeable {
    Path toPath(URI uri) throws IOException;
}
