package org.jobrunr.utils;

import java.math.BigDecimal;
import java.time.Duration;

public class DurationUtils {

    public static BigDecimal toBigDecimal(Duration duration) {
        return BigDecimal.valueOf(duration.toNanos()).scaleByPowerOfTen(-9);
    }

    public static Duration fromBigDecimal(BigDecimal bigDecimal) {
        return Duration.ofSeconds(
                bigDecimal.longValue(),
                bigDecimal.remainder(BigDecimal.ONE).movePointRight(bigDecimal.scale()).abs().longValue());
    }
}
