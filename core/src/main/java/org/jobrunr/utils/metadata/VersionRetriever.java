package org.jobrunr.utils.metadata;

import java.io.File;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class VersionRetriever {

    public static String getVersion(Class<?> clazz) {
        return selectOptional(
                getVersionFromManifest(clazz),
                getVersionFromManifestUsingJavaDefault(clazz)
        ).orElse("Unable to determine version");
    }

    private static Optional<String> getVersionFromManifest(Class<?> clazz) {
        try {
            File file = new File(clazz.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (file.isFile()) {
                JarFile jarFile = new JarFile(file);
                Manifest manifest = jarFile.getManifest();
                Attributes attributes = manifest.getMainAttributes();
                final String version = attributes.getValue("Bundle-Version");
                return Optional.of(version);
            }
        } catch (Exception e) {
            // ignore
        }
        return Optional.empty();
    }

    private static Optional<String> getVersionFromManifestUsingJavaDefault(Class<?> clazz) {
        Package aPackage = clazz.getPackage();
        if (aPackage != null) {
            String version = aPackage.getImplementationVersion();
            if (version == null) {
                version = aPackage.getSpecificationVersion();
            }
            return Optional.ofNullable(version);
        }
        return Optional.empty();
    }

    @SafeVarargs
    public static <T> Optional<T> selectOptional(Optional<T>... optionals) {
        for (Optional<T> optional : optionals) {
            if (optional.isPresent()) return optional;
        }
        return Optional.empty();
    }
}
