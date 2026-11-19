package org.jobrunr.utils;

import java.math.BigDecimal;
import java.time.Duration;

public class DurationUtils {

    private DurationUtils() {}

    public static BigDecimal toBigDecimal(Duration duration) {
        int nanos = duration.getNano();
        if (nanos == 0) return BigDecimal.valueOf(duration.getSeconds());
        return BigDecimal.valueOf(duration.getSeconds())
                .add(BigDecimal.valueOf(duration.getNano(), 9));
    }

    public static Duration fromBigDecimal(BigDecimal bigDecimal) {
        return Duration.ofSeconds(
                bigDecimal.longValue(),
                bigDecimal.remainder(BigDecimal.ONE).movePointRight(9).longValue());
    }

    public static Duration min(Duration a, Duration b) {
        if (a.compareTo(b) < 0) return a;
        return b;
    }
}
