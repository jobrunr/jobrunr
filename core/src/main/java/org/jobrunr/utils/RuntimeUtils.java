package org.jobrunr.utils;

public class RuntimeUtils {

    public static int getJvmVersion() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if (dot != -1) {
                version = version.substring(0, dot);
            }
        }
        return Integer.parseInt(version);
    }

    public static boolean isRunningFromNestedJar() {
        return RuntimeUtils.class.getProtectionDomain().getCodeSource().getLocation().toString().split("!").length > 1;
    }
}
