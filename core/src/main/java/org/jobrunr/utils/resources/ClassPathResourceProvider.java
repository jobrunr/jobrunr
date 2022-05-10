package org.jobrunr.utils.resources;

import org.jobrunr.JobRunrException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

/**
 * Class to be only used on startup to load all resources (SQL migrations and noSQL migrations) from the classpath.
 *
 * As Jar files need to be mounted as FileSystems which are static, this class uses explicit locking to ensure that only one
 * consumer can access the resources at a time. It must thus always be used in a try-with-resources block.
 */
public class ClassPathResourceProvider implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassPathResourceProvider.class);
    private static final Map<String, FileSystemProvider> fileSystemProviders = new HashMap<>();
    private static final ReentrantLock lock = new ReentrantLock();

    public ClassPathResourceProvider() {
        try {
            lock.tryLock(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Unable to open lock. Make sure the ClassPathResourceProvider is used inside a try-with-resources block?", e);
        }
    }


    public Stream<Path> listAllChildrenOnClasspath(Class<?> clazz, String... subFolder) {
        try {
            return toPathsOnClasspath(clazz, subFolder)
                    .flatMap(this::listAllChildrenOnClasspath);
        } catch (Exception e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }

    private Stream<Path> toPathsOnClasspath(Class<?> clazz, String... subFolders) {
        return toPathsOnClasspath(clazz.getPackage(), subFolders);
    }

    private Stream<Path> toPathsOnClasspath(Package pkg, String... subFolders) {
        final String joinedSubfolders = String.join("/", subFolders);
        if (joinedSubfolders.startsWith("/")) {
            return toUrls(joinedSubfolders.substring(1))
                    .map(this::toPath);
        }
        return toUrls(pkg.getName().replace(".", "/") + "/" + joinedSubfolders)
                .map(this::toPath);
    }

    private Stream<URL> toUrls(String folder) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources(folder);

            return Collections.list(resources).stream();
        } catch (IOException e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }

    private Path toPath(URL url) {
        try {
            URI uri = url.toURI();
            if ("wsjar".equals(uri.getScheme())) { // support for openliberty
                uri = new URI(uri.toString().replace("wsjar", "jar"));
            }

            FileSystemProvider fileSystemProvider = getFileSystemProvider(uri);
            return fileSystemProvider.toPath(uri);
        } catch (IOException | URISyntaxException e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }

    private Stream<Path> listAllChildrenOnClasspath(Path rootPath) {
        try {
            if (rootPath == null) return Stream.empty();
            if (Files.notExists(rootPath)) return Stream.empty();

            return Files.list(rootPath);
        } catch (IOException e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }

    private FileSystemProvider getFileSystemProvider(URI uri) {
        return this.fileSystemProviders.computeIfAbsent(uri.getScheme(), this::getFileSystemProviderByScheme);
    }

    @Override
    public void close() throws IllegalStateException {
        try {
            for (FileSystemProvider fileSystemProvider : this.fileSystemProviders.values()) {
                closeFileSystemProvider(fileSystemProvider);
            }
        } catch (Exception e) {
            LOGGER.error("Could not close FileSystemProvider", e);
            throw new IllegalStateException("Could not close FileSystemProvider", e);
        } finally {
            this.fileSystemProviders.clear();
            lock.unlock();
        }
    }

    private FileSystemProvider getFileSystemProviderByScheme(String scheme) {
        switch (scheme) {
            case "jar": return new JarFileSystemProvider();
            case "resource": return new ResourcesFileSystemProvider();
            case "file": return new PathFileSystemProvider();
            default: throw new IllegalArgumentException("Unknown FileSystem required " + scheme);
        }
    }

    private void closeFileSystemProvider(FileSystemProvider fileSystemProvider) throws IOException {
        try {
            fileSystemProvider.close();
        } catch (ClosedFileSystemException e) {
            // ignore
        }
    }
}
