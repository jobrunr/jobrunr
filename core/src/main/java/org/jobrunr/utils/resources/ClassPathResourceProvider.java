package org.jobrunr.utils.resources;

import org.jobrunr.JobRunrException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class ClassPathResourceProvider implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassPathResourceProvider.class);
    private final Map<String, FileSystemProvider> fileSystemProviders;

    public ClassPathResourceProvider() {
        this.fileSystemProviders = new HashMap<>();
    }

    public Stream<Path> listAllChildrenOnClasspath(Class<?> clazz, String... subFolder) {
        try {
            return toPathsOnClasspath(clazz, subFolder)
                    .flatMap(this::listAllChildrenOnClasspath);
        } catch (Exception e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }

    public Stream<Path> toPathsOnClasspath(String... subFolder) {
        return toPathsOnClasspath(ClassPathResourceProvider.class, subFolder);
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
                fileSystemProvider.close();
            }
        } catch (Exception e) {
            LOGGER.error("Could not close FileSystemProvider", e);
            throw new IllegalStateException("Could not close FileSystemProvider", e);
        } finally {
            this.fileSystemProviders.clear();
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
}
