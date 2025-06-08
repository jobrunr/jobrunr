package org.jobrunr.utils;

import java.nio.file.Paths;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class CIInfo {

    public enum CIType {
        NAS(true, workDir -> workDir.startsWith("/volume2")),
        MacMini(true, workDir -> workDir.startsWith("/Users/rdehuyss")),
        Other(false, StringUtils::isNullOrEmpty);

        private final String workDir;
        private final boolean isActive;
        private final boolean isCIMachine;

        CIType(boolean isCIMachine, Predicate<String> isActivePredicate) {
            this.isCIMachine = isCIMachine;
            workDir = System.getenv("CI_LOCAL_WORK_DIR");
            isActive = isActivePredicate.test(workDir);
        }

        public String getWorkDir() {
            return workDir;
        }

        public String getM2RepoDir() {
            return isCIMachine
                    ? workDir + "/m2/cache"
                    : Paths.get(System.getProperty("user.home"), ".m2").toString();
        }
    }

    public static boolean isRunningOn(CIType type) {
        return type.isActive;
    }

    public static CIType current() {
        return Stream.of(CIType.values())
                .filter(type -> type.isActive)
                .findFirst().orElseThrow();
    }

    public static boolean isRunningOnCIMachine() {
        return current().isCIMachine;
    }
}
