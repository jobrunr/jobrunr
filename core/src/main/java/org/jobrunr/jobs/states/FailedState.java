package org.jobrunr.jobs.states;

import org.jobrunr.JobRunrException;
import org.jobrunr.utils.reflection.ReflectionUtils;

import java.time.Instant;

import static org.jobrunr.utils.exceptions.Exceptions.getStackTraceAsString;

@SuppressWarnings({"FieldMayBeFinal", "CanBeFinal"}) // because of JSON-B
public class FailedState extends AbstractJobState {

    private transient Exception exception;
    private String message;
    private String exceptionType;
    private String exceptionMessage;
    private String exceptionCauseType;
    private String exceptionCauseMessage;
    private String stackTrace;
    private boolean doNotRetry;

    protected FailedState() { // for json deserialization
        this(
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            null
        );
    }

    public FailedState(String message, Exception exception) {
        this(
            message,
            exception.getClass().getName(),
            exception.getMessage(),
            hasCause(exception) ? exception.getCause().getClass().getName() : null,
            hasCause(exception) ? exception.getCause().getMessage() : null,
            getStackTraceAsString(exception),
            isProblematicAndDoNotRetry(exception),
            null
        );
        this.exception = exception;
    }

    public FailedState(String message,
                       String exceptionType,
                       String exceptionMessage,
                       String exceptionCauseType,
                       String exceptionCauseMessage,
                       String stackTrace,
                       boolean doNotRetry,
                       Instant createdAt) {
        super(StateName.FAILED, createdAt);
        this.message = message;
        this.exceptionType = exceptionType;
        this.exceptionMessage = exceptionMessage;
        this.exceptionCauseType = exceptionCauseType;
        this.exceptionCauseMessage = exceptionCauseMessage;
        this.stackTrace = stackTrace;
        this.doNotRetry = doNotRetry;
    }

    public String getMessage() {
        return message;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public String getExceptionCauseType() {
        return exceptionCauseType;
    }

    public String getExceptionCauseMessage() {
        return exceptionCauseMessage;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public boolean mustNotRetry() {
        return doNotRetry;
    }

    public Exception getException() {
        if (exception != null) return exception;
        try {
            final Class<? extends Exception> exceptionClass = ReflectionUtils.toClass(getExceptionType());
            if (getExceptionCauseType() != null) {
                final Class<? extends Exception> exceptionCauseClass = ReflectionUtils.toClass(getExceptionCauseType());
                final Exception exceptionCause = getExceptionCauseMessage() != null ? ReflectionUtils.newInstanceCE(exceptionCauseClass, getExceptionCauseMessage()) : ReflectionUtils.newInstanceCE(exceptionCauseClass);
                exceptionCause.setStackTrace(new StackTraceElement[]{});
                return getExceptionMessage() != null ? ReflectionUtils.newInstanceCE(exceptionClass, getExceptionMessage(), exceptionCause) : ReflectionUtils.newInstanceCE(exceptionClass, exceptionCause);
            } else {
                return getExceptionMessage() != null ? ReflectionUtils.newInstanceCE(exceptionClass, getExceptionMessage()) : ReflectionUtils.newInstanceCE(exceptionClass);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not reconstruct exception for class " + getExceptionType() + " and message " + getExceptionMessage(), e);
        }
    }

    private static boolean hasCause(Exception exception) {
        return exception.getCause() != null && exception.getCause() != exception;
    }

    private static boolean isProblematicAndDoNotRetry(Exception exception) {
        return exception instanceof JobRunrException && ((JobRunrException) exception).isProblematicAndDoNotRetry();
    }

}
