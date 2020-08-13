package org.jobrunr.utils;

import java.io.IOException;
import java.net.ServerSocket;

import static java.lang.String.format;

public class FreePortFinder {

    private FreePortFinder() {

    }

    public static int nextFreePort(int from) {
        int i = 0;
        int port = from;
        while (i < 10) {
            if (isLocalPortFree(port)) {
                return port;
            } else {
                port++;
                i++;
            }
        }
        throw new IllegalStateException(format("Could not find an available port starting from %d", from));
    }

    private static boolean isLocalPortFree(int port) {
        try {
            new ServerSocket(port).close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
