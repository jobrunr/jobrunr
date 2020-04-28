package org.jobrunr;

public class JobRunrException extends RuntimeException {

    public static final String SHOULD_NOT_HAPPEN_MESSAGE = "JobRunr encounterd a problematic exception. Please create a bug report (if possible, provide the code to reproduce this and the stacktrace)";

    public JobRunrException(String message) {
        super(message);
    }

    public JobRunrException(String message, Throwable cause) {
        super(message, cause);
    }

    public static JobRunrException shouldNotHappenException(String message) {
        return new JobRunrException(SHOULD_NOT_HAPPEN_MESSAGE, new IllegalStateException(message));
    }

    public static JobRunrException shouldNotHappenException(Throwable cause) {
        if (cause instanceof JobRunrException) return (JobRunrException) cause;
        if (cause.getCause() instanceof JobRunrException) return (JobRunrException) cause.getCause();
        return new JobRunrException(SHOULD_NOT_HAPPEN_MESSAGE, cause);
    }

    public static JobRunrException configurationException(String message, Throwable cause) {
        if (cause instanceof JobRunrException) return (JobRunrException) cause;
        if (cause.getCause() instanceof JobRunrException) return (JobRunrException) cause.getCause();
        return new JobRunrException(message, cause);
    }
}
