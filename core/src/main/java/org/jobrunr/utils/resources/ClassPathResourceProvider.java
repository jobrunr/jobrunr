package org.jobrunr.utils.resources;

import org.jobrunr.JobRunrException;
import org.jobrunr.configuration.JobRunr;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class to be only used on startup to load all resources (SQL migrations and noSQL migrations) from the classpath.
 * <p>
 * As Jar files need to be mounted as FileSystems which are static, this class uses explicit locking to ensure that only one
 * consumer can access the resources at a time. It must thus always be used in a try-with-resources block.
 */
public class ClassPathResourceProvider implements AutoCloseable {

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
        return listAllChildrenOnClasspath(toFolder(clazz, subFolder));
    }

    public Stream<Path> listAllChildrenOnClasspath(String path) {
        try {
            return toPathsOnClasspath(path)
                    .flatMap(this::listAllChildrenOnClasspath);
        } catch (Exception e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }

    private Stream<Path> toPathsOnClasspath(String path) {
        return toUrls(path)
                .map(this::toPath);
    }

    private Stream<URL> toUrls(String folder) {
        try {
            if (System.getProperty("org.graalvm.nativeimage.imagecode") != null) {
                return Stream.of(new URL("resource:/" + folder));
            }

            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> contextClassLoaderResources = classLoader.getResources(folder);
            if (contextClassLoaderResources.hasMoreElements()) {
                return Collections.list(contextClassLoaderResources).stream();
            }

            Enumeration<URL> classLoaderResources = JobRunr.class.getClassLoader().getResources(folder);
            if (classLoaderResources.hasMoreElements()) {
                return Collections.list(classLoaderResources).stream();
            }
            return Stream.empty();
        } catch (IOException e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }

    private Path toPath(URL url) {
        try {
            URI uri = url.toURI();
            if ("wsjar".equals(uri.getScheme())) { // support for openliberty
                uri = new URI(uri.toString().replace("wsjar", "jar"));
            } else if ("vfs".equals(uri.getScheme())) {    // support for Jboss/Wildfly
                uri = handleVfsScheme(url);
            }
            FileSystemProvider fileSystemProvider = getFileSystemProvider(uri);
            return fileSystemProvider.toPath(uri);
        } catch (IOException | URISyntaxException e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }

    private URI handleVfsScheme(URL url) throws IOException {
        // Reflection as we cannot afford a dependency to Jboss/WildFly
        Object virtualFile = url.openConnection().getContent();
        Class virtualFileClass = virtualFile.getClass();

        try {
            Method getChildrenRecursivelyMethod = virtualFileClass.getMethod("getChildrenRecursively");
            Method getPhysicalFileMethod = virtualFileClass.getMethod("getPhysicalFile");

            List virtualFiles = (List) getChildrenRecursivelyMethod.invoke(virtualFile);
            for (Object child : virtualFiles) {
                getPhysicalFileMethod.invoke(child);// side effect: create real-world files
            }
            File rootDir = (File) getPhysicalFileMethod.invoke(virtualFile);
            return URI.create(VfsFilesystemProvider.SCHEME + rootDir.toURI());
        } catch (IllegalArgumentException | ReflectiveOperationException e) {
            throw JobRunrException.shouldNotHappenException(new RuntimeException("Can not extract files from vfs!", e));
        }
    }

    private Stream<Path> listAllChildrenOnClasspath(Path rootPath) {
        try {
            if (rootPath == null || Files.notExists(rootPath)) return Stream.empty();

            try (Stream<Path> stream = Files.list(rootPath)) {
                return stream.collect(Collectors.toList()).stream(); // still return a stream but make sure to close the resource
            }
        } catch (IOException e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }

    private FileSystemProvider getFileSystemProvider(URI uri) {
        return fileSystemProviders.computeIfAbsent(uri.getScheme(), this::getFileSystemProviderByScheme);
    }

    @Override
    public void close() throws IllegalStateException {
        try {
            for (FileSystemProvider fileSystemProvider : this.fileSystemProviders.values()) {
                closeFileSystemProvider(fileSystemProvider);
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(ClassPathResourceProvider.class).error("Could not close FileSystemProvider", e);
            throw new IllegalStateException("Could not close FileSystemProvider", e);
        } finally {
            this.fileSystemProviders.clear();
            lock.unlock();
        }
    }

    private FileSystemProvider getFileSystemProviderByScheme(String scheme) {
        switch (scheme) {
            case "jar":
                return new JarFileSystemProvider();
            case "resource":
                return new ResourcesFileSystemProvider();
            case "file":
                return new PathFileSystemProvider();
            case "vfs":
                return new VfsFilesystemProvider();
            default:
                throw new IllegalArgumentException("Unknown FileSystem required " + scheme);
        }
    }

    private void closeFileSystemProvider(FileSystemProvider fileSystemProvider) throws IOException {
        try {
            fileSystemProvider.close();
        } catch (ClosedFileSystemException e) {
            // ignore
        }
    }


    private static String toFolder(Class<?> clazz, String... subFolders) {
        return toFolder(clazz.getPackage(), subFolders);
    }

    private static String toFolder(Package pkg, String... subFolders) {
        final String joinedSubFolders = String.join("/", subFolders);
        if (joinedSubFolders.startsWith("/")) {
            return joinedSubFolders.substring(1);
        }
        return pkg.getName().replace(".", "/") + "/" + joinedSubFolders;
    }
}
