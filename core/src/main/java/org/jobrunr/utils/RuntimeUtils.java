package org.jobrunr.utils;

import static org.jobrunr.utils.StringUtils.substringBefore;

public class RuntimeUtils {

    private RuntimeUtils() {
    }

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
        version = substringBefore(version, "-");
        return Integer.parseInt(version);
    }

    public static boolean isRunningFromNestedJar() {
        return RuntimeUtils.class.getProtectionDomain().getCodeSource().getLocation().toString().split("!").length > 1;
    }
}
