package org.jobrunr.scheduling;

import io.quarkus.runtime.annotations.Recorder;

// why: Quarkus expects to inject a method parameter whose type is annoated with @Recorder for @Record build steps
// Logic is implemented in AsyncJobPostProcessor
@Recorder
public class AsyncJobValidationRecorder {
}
