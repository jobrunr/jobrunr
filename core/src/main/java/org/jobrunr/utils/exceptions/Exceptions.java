package org.jobrunr.utils.exceptions;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Function;
import java.util.function.Supplier;

public class Exceptions {

    public static boolean hasCause(Throwable t, Class<? extends Throwable> exceptionClass) {
        if (exceptionClass.isAssignableFrom(t.getClass())) return true;
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
        return retryOnException(supplier, maxRetries, 20L);
    }

    public static <T> T retryOnException(Supplier<T> supplier, int maxRetries, long timeSeed) {
        return retryOnException(supplier, e -> true, maxRetries, timeSeed);
    }

    public static <T, E extends RuntimeException> T retryOnException(Supplier<T> supplier, Function<E, Boolean> retry, int maxRetries, long timeSeed) {
        int count = 0;
        while (count <= maxRetries) {
            try {
                Thread.sleep(count * timeSeed);
                return supplier.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (RuntimeException e) {
                E exception = (E) e;
                if (!retry.apply(exception)) throw e;
                if (++count >= maxRetries) throw e;
            }
        }
        throw new IllegalStateException("Cannot happen");
    }

    public static void retryOnException(Runnable runnable, int maxRetries) {
        retryOnException(runnable, e -> true, maxRetries);
    }

    public static void retryOnException(Runnable runnable, int maxRetries, long timeSeed) {
        retryOnException(runnable, e -> true, maxRetries, timeSeed);
    }

    public static <E extends RuntimeException> void retryOnException(Runnable runnable, Function<E, Boolean> retry, int maxRetries) {
        retryOnException(runnable, retry, maxRetries, 20);
    }

    public static <E extends RuntimeException> void retryOnException(Runnable runnable, Function<E, Boolean> retry, int maxRetries, long timeSeed) {
        int count = 0;
        while (count <= maxRetries) {
            try {
                Thread.sleep(count * timeSeed);
                runnable.run();
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (RuntimeException e) {
                E exception = (E) e;
                if (!retry.apply(exception)) throw e;
                if (++count >= maxRetries) throw e;
            }
        }
    }
}