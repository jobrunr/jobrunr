package org.jobrunr.jobs.details;

import org.jobrunr.JobRunrException;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;

import static org.jobrunr.JobRunrException.shouldNotHappenException;
import static org.jobrunr.utils.reflection.ReflectionUtils.makeAccessible;

public class SerializedLambdaConverter {

    public <T> SerializedLambda toSerializedLambda(T value) {
        if (!value.getClass().isSynthetic()) {
            throw new IllegalArgumentException("Please provide a lambda expression (e.g. BackgroundJob.enqueue(() -> myService.doWork()) instead of an actual implementation.");
        }

        if (!(value instanceof Serializable)) {
            throw new JobRunrException("The lambda you provided is not Serializable. Please make sure your functional interface is Serializable or use the JobLambda interface instead.");
        }

        try {
            Method writeReplaceMethod = value.getClass().getDeclaredMethod("writeReplace");
            makeAccessible(writeReplaceMethod);
            return (SerializedLambda) writeReplaceMethod.invoke(value);
        } catch (Exception shouldNotHappen) {
            throw shouldNotHappenException(shouldNotHappen);
        }
    }
}
