package org.jobrunr.scheduling;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class AsyncJobValidationRecorder {
    public void validate(boolean hasAsyncJobAnnotation, boolean hasVoidAsReturnType, String methodName) {
        if (hasAsyncJobAnnotation && !hasVoidAsReturnType) {
            throw new IllegalArgumentException("An @AsyncJob cannot have a return value. " + methodName + " is defined as an @AsyncJob but has a return value.");
        }
    }
}
