package org.jobrunr.utils;

import org.jobrunr.server.threadpool.CustomizableThreadFactory;

import java.util.concurrent.ThreadFactory;

public class ThreadUtils {

    private ThreadUtils() {
    }

    public static ThreadFactory daemonThreadFactory(String threadPrefix) {
        return new CustomizableThreadFactory(threadPrefix, true);
    }

    public static ThreadFactory withPrefix(String threadPrefix) {
        return new CustomizableThreadFactory(threadPrefix, false);
    }
}
