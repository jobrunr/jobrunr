package org.jobrunr.utils;

import java.time.Instant;

public class InstantUtils {

    private InstantUtils() {
    }

    public static boolean isInstantInPeriod(Instant instant, Instant startOfPeriod, Instant endOfPeriod) {
        return isInstantAfterOrEqualTo(instant, startOfPeriod) && instant.isBefore(endOfPeriod);
    }

    public static boolean isInstantBeforeOrEqualTo(Instant instant, Instant other) {
        return !instant.isAfter(other);
    }

    public static boolean isInstantAfterOrEqualTo(Instant instant, Instant other) {
        return !instant.isBefore(other);
    }
}
