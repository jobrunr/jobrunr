package org.jobrunr.utils.exceptions;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Exceptions {

    public static boolean hasCause(Throwable t, Class<? extends Throwable> exceptionClass) {
        if (t.getClass().isAssignableFrom(exceptionClass)) return true;
        while (t.getCause() != null) {
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
}
