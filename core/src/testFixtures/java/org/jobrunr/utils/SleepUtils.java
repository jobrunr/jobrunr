package org.jobrunr.utils;

import java.util.concurrent.TimeUnit;

public class SleepUtils {

    private SleepUtils() {
    }

    public static void sleep(long time, TimeUnit timeUnit) {
        sleep(timeUnit.toMillis(time));
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
