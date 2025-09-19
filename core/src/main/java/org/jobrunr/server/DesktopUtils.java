package org.jobrunr.server;

import org.jobrunr.utils.reflection.ReflectionUtils;

import java.net.URL;
import java.net.URLClassLoader;
import java.time.Duration;
import java.time.Instant;

import static org.jobrunr.utils.VersionNumber.JAVA_VERSION;

public class DesktopUtils {

    private static Internal internal;

    static {
        internal = new Java8Internal();
        if (JAVA_VERSION.hasMajorVersionHigherOrEqualTo(11) && ReflectionUtils.classExists("java.awt.Desktop")) {
            try (URLClassLoader classLoader = new URLClassLoader(new URL[]{DesktopUtils.class.getResource("/org/jobrunr/server/Java11OrHigherInternalDesktopUtil.class")})) {
                Class<?> loadedClass = classLoader.loadClass("org.jobrunr.server.Java11OrHigherInternalDesktopUtil");
                Object obj = loadedClass.getDeclaredConstructor().newInstance();
                if (obj instanceof Internal) {
                    DesktopUtils.internal = (Internal) obj;
                }
            } catch (Exception e) {
                // Nothing we can do...
            }
        }
    }

    public static boolean systemSupportsSleepDetection() {
        return internal.supportsSystemSleepDetection();
    }

    public static Instant getLastSystemAwakeTime() {
        return internal.getLastSystemAwakeTime();
    }

    public static boolean hasSystemSleptRecently() {
        return hasSystemSleptRecently(Duration.ofMinutes(5));
    }

    public static boolean hasSystemSleptRecently(Duration duration) {
        return getLastSystemAwakeTime().plus(duration).isAfter(Instant.now());
    }

    public interface Internal {

        boolean supportsSystemSleepDetection();

        Instant getLastSystemAwakeTime();

    }

    public static class Java8Internal implements Internal {

        @Override
        public boolean supportsSystemSleepDetection() {
            return false;
        }

        @Override
        public Instant getLastSystemAwakeTime() {
            return Instant.MIN;
        }

    }
}
