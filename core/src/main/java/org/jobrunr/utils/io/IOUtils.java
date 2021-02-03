package org.jobrunr.utils.io;

import java.io.*;

public class IOUtils {

    public static void copyStreamNoException(InputStream input, Writer output) {
        try {
            copyStream(input, output);
        } catch (IOException e) {
            // swallow e
        }
    }

    public static void copyStream(InputStream input, Writer output) throws IOException {
        try (InputStreamReader inputStreamReader = new InputStreamReader(input)) {
            char[] buffer = new char[1024]; // Adjust if you want
            int bytesRead;
            while ((bytesRead = inputStreamReader.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        }
    }

    public static void copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024]; // Adjust if you want
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }
}
