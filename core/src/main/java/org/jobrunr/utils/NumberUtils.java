package org.jobrunr.utils;

public class NumberUtils {

    private NumberUtils() {
    }

    public static Long parseLong(String string) {
        return parseLong(string, 0L);
    }

    public static Long parseLong(String string, Long defaultValueIfNull) {
        return string != null ? Long.valueOf(string) : defaultValueIfNull;
    }
}
