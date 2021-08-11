package org.jobrunr.utils;

import java.util.ArrayList;
import java.util.List;

public class GCUtils {

    public static void simulateStopTheWorldGC(long howLongInMillis) {
        new MemoryExhauster(howLongInMillis).start();
        callFullGCFor(howLongInMillis);
    }

    private static void callFullGCFor(long millis) {
        long timestamp = System.currentTimeMillis();
        while (timestamp + millis > System.currentTimeMillis()) {
            System.gc();
        }
    }

    public static class MemoryExhauster extends Thread {

        private final long forHowLong;

        public MemoryExhauster(long howLongInMillis) {
            this.forHowLong = howLongInMillis;
        }

        @Override
        public void run() {
            long timestamp = System.currentTimeMillis();
            while (timestamp + forHowLong > System.currentTimeMillis()) {
                tryToAllocateAllAvailableMemory();
            }
        }

        /**
         * This method tries to allocate maximum available memory in runtime,
         * and is catching an OutOfMemoryError.
         */
        public static void tryToAllocateAllAvailableMemory() {
            try {
                final List<Object[]> allocations = new ArrayList<>();
                int size;
                while ((size = (int) Runtime.getRuntime().freeMemory()) > 0) {
                    Object[] part = new Object[Math.min(size, Integer.MAX_VALUE)];
                    allocations.add(part);
                }
            } catch (OutOfMemoryError e) {
                System.out.println("catch expected exception: " + e.getMessage());
            }
        }
    }
}
