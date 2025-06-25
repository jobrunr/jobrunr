package org.jobrunr.utils;

import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class StringUtils {

    private StringUtils() {
    }

    public static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static boolean isNotNullOrEmpty(String s) {
        return !isNullOrEmpty(s);
    }

    public static boolean isNullEmptyOrBlank(String s) {
        return isNullOrEmpty(s) || isNullOrEmpty(s.trim());
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

    /**
     * Returns the last matched String between the given open and close String.
     *
     * @param s     the String containing the substring, may be null
     * @param open  the String before the substring, may not be null
     * @param close the String after the substring, may not be null
     * @return Returns the last matched String between the given open and close String when possible or null.
     */
    public static String lastMatchedSubstringBetween(String s, String open, String close) {
        if (s.contains(open) && s.contains(close)) {
            return substringBefore(substringAfterLast(s, open), close);
        }
        return null;
    }

    /**
     * Returns the String between the given open and close String. If the closing String is not present, it will return everything after the opening String.
     *
     * @param s     the String containing the substring, may be null
     * @param open  the String before the substring, may not be null
     * @param close the String after the substring, may not be null
     * @return Returns the String between the given open and close String when possible or everything after the opening String.
     */
    public static String lenientSubstringBetween(String s, String open, String close) {
        if (s != null && s.contains(open)) {
            String result = substringAfter(s, open);
            if (result.contains(close)) {
                return substringBefore(result, close);
            }
            return result;
        }
        return null;
    }

    public static String md5Checksum(String input) {
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.reset();
            m.update(input.getBytes(UTF_8));
            byte[] digest = m.digest();
            BigInteger bigInt = new BigInteger(1, digest);
            String hashResult = bigInt.toString(16);
            while (hashResult.length() < 32) {
                hashResult = "0" + hashResult;
            }
            return hashResult;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 Hashing algorithm not found.", e);
        }
    }

    public static String urlEncode(String string) {
        try {
            return URLEncoder.encode(string, UTF_8.toString());
        } catch (Exception e) {
            return string;
        }
    }
}
