package org.jobrunr.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static java.lang.Math.min;

public class NetworkUtils {

    public static String getHostName() {
        try {
            String hostName = InetAddress.getLocalHost().getHostName();
            return hostName.substring(0, min(hostName.length(), 127));
        } catch (UnknownHostException e) {
            return "Unable to determine hostname";
        }
    }
}
