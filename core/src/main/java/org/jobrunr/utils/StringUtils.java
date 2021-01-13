package org.jobrunr.utils;

public class StringUtils {

    private StringUtils() {
    }

    public static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static boolean isNotNullOrEmpty(String s) {
        return !isNullOrEmpty(s);
    }

    public static String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    public static String substringBefore(String s, String splitter) {
        return s.substring(0, s.indexOf(splitter));
    }

    public static String substringAfter(String s, String splitter) {
        return s.substring(s.indexOf(splitter) + 1);
    }

    public static String substringBeforeLast(String s, String splitter) {
        return s.substring(0, s.lastIndexOf(splitter));
    }

    public static String substringAfterLast(String s, String splitter) {
        return s.substring(s.lastIndexOf(splitter) + 1);
    }
}
