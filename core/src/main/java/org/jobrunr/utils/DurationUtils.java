package org.jobrunr.utils;

import java.math.BigDecimal;
import java.time.Duration;

public class DurationUtils {

    private DurationUtils() {}

    public static BigDecimal toBigDecimal(Duration duration) {
        return BigDecimal.valueOf(duration.toNanos()).scaleByPowerOfTen(-9);
    }

    public static Duration fromBigDecimal(BigDecimal bigDecimal) {
        return Duration.ofNanos(bigDecimal.movePointRight(9).longValue());
    }

    public static Duration min(Duration a, Duration b) {
        if (a.compareTo(b) < 0) return a;
        return b;
    }
}
