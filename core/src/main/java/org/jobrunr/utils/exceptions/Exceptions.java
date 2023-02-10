package org.jobrunr.utils.exceptions;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Supplier;

public class Exceptions {

    public static boolean hasCause(Throwable t, Class<? extends Throwable> exceptionClass) {
        if (t.getClass().isAssignableFrom(exceptionClass)) return true;
        if (t.getCause() != null) {
            return hasCause(t.getCause(), exceptionClass);
        }
        return false;
    }

    public static String getStackTraceAsString(Throwable exception) {
        StringWriter sw = new StringWriter();
        exception.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingConsumer<T> {
        void accept(T t) throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingBiConsumer<T, U> {
        void accept(T t, U u) throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingFunction<T, R> {
        R apply(T t) throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingBiFunction<T, U, R> {
        R apply(T t, U u) throws Exception;
    }

    public static <T> T retryOnException(Supplier<T> supplier, int maxRetries) {
        int count = 0;
        while (count <= maxRetries) {
            try {
                Thread.sleep(count * 20L);
                return supplier.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (RuntimeException e) {
                if (++count >= maxRetries) throw e;
            }
        }
        throw new IllegalStateException("Cannot happen");
    }

    public static void retryOnException(Runnable runnable, int maxRetries) {
        int count = 0;
        while (count <= maxRetries) {
            try {
                Thread.sleep(count * 20L);
                runnable.run();
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (RuntimeException e) {
                if (++count >= maxRetries) throw e;
            }
        }
    }
}
