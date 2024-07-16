package org.jobrunr.utils;

import java.time.Instant;

public class InstantUtils {

    private InstantUtils() {
    }

    public static boolean isInstantBeforeOrEqualToOther(Instant instant, Instant other) {
        return !instant.isAfter(other);
    }

    public static boolean isInstantAfterOrEqualToOther(Instant instant, Instant other) {
        return !instant.isBefore(other);
    }
}
