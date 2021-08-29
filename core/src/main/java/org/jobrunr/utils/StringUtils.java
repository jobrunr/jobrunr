package org.jobrunr.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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
        int endIndex = s.indexOf(splitter);
        if (endIndex >= 0) {
            return s.substring(0, endIndex);
        }
        return s;
    }

    public static String substringAfter(String s, String splitter) {
        final int indexOf = s.indexOf(splitter);
        return indexOf >= 0 ? s.substring(indexOf + splitter.length()) : null;
    }

    public static String substringBeforeLast(String s, String splitter) {
        return s.substring(0, s.lastIndexOf(splitter));
    }

    public static String substringAfterLast(String s, String splitter) {
        return s.substring(s.lastIndexOf(splitter) + 1);
    }

    public static String substringBetween(String s, String open, String close) {
        if (s.contains(open) && s.contains(close)) {
            return substringBefore(substringAfter(s, open), close);
        }
        return null;
    }

    public static String urlEncode(String string) {
        try {
            return URLEncoder.encode(string, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            return string;
        }
    }
}
