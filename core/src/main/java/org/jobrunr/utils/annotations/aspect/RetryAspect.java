package org.jobrunr.utils.annotations.aspect;

import org.jobrunr.utils.annotations.Retry;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class RetryAspect {
    public static Object retry(Object obj, Method method, Object[] args) throws Throwable {
        Retry retryAnnotation = method.getAnnotation(Retry.class);
        int maxAttempts = retryAnnotation.maxAttempts();
        long delay = retryAnnotation.delayMs();

        int attempt = 0;
        Throwable lastException = null;

        while (attempt < maxAttempts) {
            try {
                return method.invoke(obj, args);
            } catch (InvocationTargetException e) {
                lastException = e.getTargetException();
                attempt++;
                Thread.sleep(delay);
            }
        }

        throw lastException;
    }
}