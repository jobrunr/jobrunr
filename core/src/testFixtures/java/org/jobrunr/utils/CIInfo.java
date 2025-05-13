package org.jobrunr.utils;

import static org.jobrunr.utils.StringUtils.isNotNullOrEmpty;
import static org.jobrunr.utils.StringUtils.isNullOrEmpty;

public class CIInfo {

    private CIInfo() {
    }

    public enum CIType {
        NAS,
        MacMini,
        Other
    }

    public static boolean isRunningOn(CIType type) {
        String droneWorkDir = System.getenv("DRONE_WORK_DIR");
        if (isNotNullOrEmpty(droneWorkDir)) {
            if (droneWorkDir.startsWith("/volume2") && CIType.NAS.equals(type)) return true;
            if (droneWorkDir.startsWith("/Users/rdehuyss") && CIType.MacMini.equals(type)) return true;
        }
        return isNullOrEmpty(droneWorkDir) && CIType.Other.equals(type);
    }
}
