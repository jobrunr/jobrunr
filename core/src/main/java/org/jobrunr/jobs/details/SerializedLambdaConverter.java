package org.jobrunr.jobs.details;

import org.jobrunr.JobRunrException;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;

import static org.jobrunr.JobRunrException.shouldNotHappenException;
import static org.jobrunr.utils.reflection.ReflectionUtils.makeAccessible;

public class SerializedLambdaConverter {

    private static final ClassValue<Method> WRITE_REPLACE_METHOD_CACHE = new ClassValue<Method>() {
        @Override
        protected Method computeValue(Class<?> type) {
            try {
                Method method = type.getDeclaredMethod("writeReplace");
                makeAccessible(method);
                return method;
            } catch (Exception e) {
                throw shouldNotHappenException(e);
            }
        }
    };

    private SerializedLambdaConverter() {}

    public static <T> SerializedLambda toSerializedLambda(T value) {
        if (!value.getClass().isSynthetic()) {
            throw new IllegalArgumentException("Please provide a lambda expression (e.g. BackgroundJob.enqueue(() -> myService.doWork()) instead of an actual implementation.");
        }

        if (!(value instanceof Serializable)) {
            throw new JobRunrException("The lambda you provided is not Serializable. Please make sure your functional interface is Serializable or use the JobLambda interface instead.");
        }

        try {
            Method writeReplaceMethod = WRITE_REPLACE_METHOD_CACHE.get(value.getClass());
            return (SerializedLambda) writeReplaceMethod.invoke(value);
        } catch (Exception e) {
            throw shouldNotHappenException(e);
        }
    }
}
