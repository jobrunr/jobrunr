package org.jobrunr.utils;

import java.math.BigDecimal;

public class NumberUtils {

    private NumberUtils() {
    }

    public static Long parseLong(String string) {
        return parseLong(string, 0L);
    }

    public static boolean isZero(BigDecimal bigDecimal) {
        if (bigDecimal == null) return false;
        return bigDecimal.compareTo(BigDecimal.ZERO) == 0;
    }

    public static Long parseLong(String string, Long defaultValueIfNull) {
        return string != null ? Long.valueOf(string) : defaultValueIfNull;
    }
}
