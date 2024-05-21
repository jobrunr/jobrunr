package org.jobrunr.utils;

import java.io.InputStream;
import java.net.URL;
import java.util.jar.Manifest;

public class JarUtils {

    private JarUtils() {

    }

    public static String getVersion(Class<?> clazz) {
        String version = clazz.getPackage().getImplementationVersion();
        if (version != null) return version;

        return getManifestAttributeValue(clazz, "Bundle-Version");
    }

    public static String getManifestAttributeValue(Class<?> clazz, String attributeName) {
        return getManifest(clazz).getMainAttributes().getValue(attributeName);
    }

    private static Manifest getManifest(Class<?> clazz) {
        String resource = "/" + clazz.getName().replace(".", "/") + ".class";
        String fullPath = clazz.getResource(resource).toString();
        String archivePath = fullPath.substring(0, fullPath.length() - resource.length());
        if (archivePath.endsWith("\\WEB-INF\\classes") || archivePath.endsWith("/WEB-INF/classes")) {
            archivePath = archivePath.substring(0, archivePath.length() - "/WEB-INF/classes".length()); // Required for wars
        }

        try (InputStream input = new URL(archivePath + "/META-INF/MANIFEST.MF").openStream()) {
            return new Manifest(input);
        } catch (Exception e) {
            throw new RuntimeException("Loading MANIFEST for class " + clazz + " failed!", e);
        }
    }
}
