package org.jobrunr.scheduling.exceptions;

import org.jobrunr.JobRunrException;

import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

public class MethodNotFoundException extends JobRunrException {

    public MethodNotFoundException(Class<?> clazz, String methodName, Class<?>[] parameterTypes) {
        super(clazz + "." + methodName + "(" + Stream.of(parameterTypes).map(Class::getName).collect(joining(",")) + ")");
    }
}
