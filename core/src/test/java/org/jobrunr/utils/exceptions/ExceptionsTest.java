package org.jobrunr.utils.exceptions;

import org.jobrunr.storage.StorageException;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ExceptionsTest {

    @Test
    void hasCauseReturnsTrueIfExceptionHasGivenCause() {
        final boolean hasInterruptedExceptionAsCause = Exceptions.hasCause(new RuntimeException(new InterruptedException()), InterruptedException.class);
        assertThat(hasInterruptedExceptionAsCause).isTrue();
    }

    @Test
    void hasCauseReturnsFalseIfExceptionDoesNotHaveGivenCause() {
        final boolean hasInterruptedExceptionAsCause = Exceptions.hasCause(new RuntimeException(new IllegalStateException()), InterruptedException.class);
        assertThat(hasInterruptedExceptionAsCause).isFalse();
    }

    @Test
    void retryOnExceptionForRunnable() {
        assertThatCode(() -> Exceptions.retryOnException(() -> System.out.println("Will run"), 5)).doesNotThrowAnyException();
        assertThatCode(() -> Exceptions.retryOnException(new RunnableThrowingRuntimeException(), 5)).isInstanceOf(StorageException.class);
    }

    @Test
    void retryOnExceptionForSupplier() {
        assertThatCode(() -> Exceptions.retryOnException(() -> "my string from supplier", 5)).doesNotThrowAnyException();
        assertThatCode(() -> Exceptions.retryOnException(new SupplierThrowingRuntimeException(), 5)).isInstanceOf(StorageException.class);
    }

    private static class RunnableThrowingRuntimeException implements Runnable {

        @Override
        public void run() {
            throw new StorageException("A storage exception occurred");
        }
    }

    private static class SupplierThrowingRuntimeException implements Supplier<String> {

        @Override
        public String get() {
            throw new StorageException("A storage exception occurred");
        }
    }
}