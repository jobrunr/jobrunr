package org.jobrunr.utils.exceptions;

import org.jobrunr.server.JobActivatorShutdownException;
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
    void hasCauseReturnsFalseIfExceptionDoesNotHaveGivenCause1() {
        final boolean hasInterruptedExceptionAsCause = Exceptions.hasCause(new RuntimeException(new IllegalStateException()), InterruptedException.class);
        assertThat(hasInterruptedExceptionAsCause).isFalse();
    }

    @Test
    void hasCauseReturnsFalseIfExceptionDoesNotHaveGivenCause2() {
        final boolean hasInterruptedExceptionAsCause = Exceptions.hasCause(new JobActivatorShutdownException("Shutdown", new Exception()), InterruptedException.class);
        assertThat(hasInterruptedExceptionAsCause).isFalse();
    }

    @Test
    void retryOnExceptionForRunnable() {
        assertThatCode(() -> Exceptions.retryOnException(() -> System.out.println("Will run"), 5)).doesNotThrowAnyException();
        assertThatCode(() -> Exceptions.retryOnException(RunnableForStorageException.AlwaysThrows(), 5)).isInstanceOf(StorageException.class);
        assertThatCode(() -> Exceptions.retryOnException(RunnableForStorageException.ThrowsAfterAttempt(4), 5)).doesNotThrowAnyException();
    }

    @Test
    void retryOnExceptionForSupplier() {
        assertThatCode(() -> Exceptions.retryOnException(() -> "my string from supplier", 5)).doesNotThrowAnyException();
        assertThatCode(() -> Exceptions.retryOnException(SupplierForStorageException.AlwaysThrows(), 5)).isInstanceOf(StorageException.class);
        assertThatCode(() -> Exceptions.retryOnException(SupplierForStorageException.ThrowsAfterAttempt(4), 5)).doesNotThrowAnyException();
    }

    private static class RunnableForStorageException implements Runnable {
        private int attempts = 0;
        private final int maxRetries;

        static RunnableForStorageException AlwaysThrows() {
            return new RunnableForStorageException(Integer.MAX_VALUE);
        }

        static RunnableForStorageException ThrowsAfterAttempt(int attempt) {
            return new RunnableForStorageException(attempt);
        }

        private RunnableForStorageException(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        @Override
        public void run() {
            attempts++;
            if (attempts < maxRetries) {
                throw new StorageException("A storage exception occurred");
            }
        }
    }

    private static class SupplierForStorageException implements Supplier<String> {
        private int attempts = 0;
        private final int maxRetries;

        static SupplierForStorageException AlwaysThrows() {
            return new SupplierForStorageException(Integer.MAX_VALUE);
        }

        static SupplierForStorageException ThrowsAfterAttempt(int attempt) {
            return new SupplierForStorageException(attempt);
        }

        private SupplierForStorageException(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        @Override
        public String get() {
            attempts++;
            if (attempts < maxRetries) {
                throw new StorageException("A storage exception occurred");
            }
            return "some value";
        }
    }
}