package org.jobrunr.utils.annotations;

import org.jobrunr.utils.annotations.aspect.RetryAspect;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RetryTest {
    private static final int MAX_ATTEMPTS = 3;
    private static final long DELAY_MS = 1;

    private int counter = 0;

    @Retry(maxAttempts = MAX_ATTEMPTS, delayMs = DELAY_MS)
    public void testSuccessOperation() {
        counter++;
        System.out.println("Attempt " + counter);

        if (counter < MAX_ATTEMPTS) {
            throw new RuntimeException("Simulated failure");
        }

        System.out.println("Operation succeeded");
    }

    @Retry(maxAttempts = MAX_ATTEMPTS, delayMs = DELAY_MS)
    public void testFailureOperation() {
        counter++;
        System.out.println("Attempt " + counter);
        throw new RuntimeException("Simulated failure");
    }


    @Test
    public void testRetrySuccess() throws Throwable {
        Method method = RetryTest.class.getMethod("testSuccessOperation");
        RetryAspect.retry(this, method, null);
        assertEquals(MAX_ATTEMPTS, counter);
    }

    @Test
    public void testRetryFailure() throws NoSuchMethodException {
        counter = 0;
        Method method = RetryTest.class.getMethod("testFailureOperation");

        assertThrows(RuntimeException.class, () -> {
            RetryAspect.retry(this, method, null);
        });

        assertEquals(MAX_ATTEMPTS, counter);
    }
}