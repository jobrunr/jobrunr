package org.jobrunr.utils.uuid;

/*
 * MIT License
 *
 * Copyright (c) 2018-2022 Fabio Lima
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/**
 * Utility class that contains many static methods for byte handling.
 */
public final class ByteUtil {

    private ByteUtil() {
    }

    /**
     * Get a number from a given array of bytes.
     *
     * @param bytes a byte array
     * @return a long
     */
    public static long toNumber(final byte[] bytes) {
        return toNumber(bytes, 0, bytes.length);
    }

    /**
     * Get a number from a given array of bytes.
     *
     * @param bytes a byte array
     * @param start first byte of the array
     * @param end   last byte of the array (exclusive)
     * @return a long
     */
    public static long toNumber(final byte[] bytes, final int start, final int end) {
        long result = 0;
        for (int i = start; i < end; i++) {
            result = (result << 8) | (bytes[i] & 0xffL);
        }
        return result;
    }

    /**
     * Get a hexadecimal string from given array of bytes.
     *
     * @param bytes byte array
     * @return a string
     */
    public static String toHexadecimal(final byte[] bytes) {

        final char[] chars = new char[bytes.length * 2];
        for (int i = 0, j = 0; i < bytes.length; i++, j += 2) {
            final int v = bytes[i] & 0xff;
            chars[j] = toHexChar(v >>> 4);
            chars[j + 1] = toHexChar(v & 0x0f);
        }
        return new String(chars);
    }

    /**
     * Get a hexadecimal from a number value.
     *
     * @param number a number
     * @return a char
     */
    private static char toHexChar(final int number) {
        if (number >= 0x00 && number <= 0x09) {
            // ASCII codes from 0 to 9
            return (char) (0x30 + number);
        } else if (number >= 0x0a && number <= 0x0f) {
            // ASCII codes from 'a' to 'f'
            return (char) (0x57 + number);
        }
        return 0;
    }
}
