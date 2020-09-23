package org.jobrunr;

public class JobRunrException extends RuntimeException {

    public static final String SHOULD_NOT_HAPPEN_MESSAGE = "JobRunr encountered a problematic exception. Please create a bug report (if possible, provide the code to reproduce this and the stacktrace)";
    public static final String INVALID_LAMBDA_MESSAGE = "The lambda you provided is not valid.";

    private final boolean doNotRetry;

    public JobRunrException(String message) {
        this(message, false);
    }

    public JobRunrException(String message, Throwable cause) {
        this(message, false, cause);
    }

    public JobRunrException(String message, boolean doNotRetry) {
        super(message);
        this.doNotRetry = doNotRetry;
    }

    public JobRunrException(String message, boolean doNotRetry, Throwable cause) {
        super(message, cause);
        this.doNotRetry = doNotRetry;
    }

    public boolean isProblematicAndDoNotRetry() {
        return doNotRetry;
    }

    public static JobRunrException shouldNotHappenException(String message) {
        return new JobRunrException(SHOULD_NOT_HAPPEN_MESSAGE, new IllegalStateException(message));
    }

    public static JobRunrException invalidLambdaException(Exception exception) {
        return new JobRunrException(INVALID_LAMBDA_MESSAGE, exception);
    }

    public static JobRunrException shouldNotHappenException(Throwable cause) {
        if (cause instanceof JobRunrException) return (JobRunrException) cause;
        if (cause.getCause() instanceof JobRunrException) return (JobRunrException) cause.getCause();
        return new JobRunrException(SHOULD_NOT_HAPPEN_MESSAGE, cause);
    }

    public static JobRunrException configurationException(String message) {
        return new JobRunrException(message);
    }

    public static JobRunrException problematicConfigurationException(String message) {
        return new JobRunrException(message, true);
    }

    public static JobRunrException problematicException(String message, Throwable cause) {
        return new JobRunrException(message, true, cause);
    }

    public static JobRunrException configurationException(String message, Throwable cause) {
        if (cause instanceof JobRunrException) return (JobRunrException) cause;
        if (cause.getCause() instanceof JobRunrException) return (JobRunrException) cause.getCause();
        return new JobRunrException(message, cause);
    }
}
