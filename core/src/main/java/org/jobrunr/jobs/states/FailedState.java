package org.jobrunr.jobs.states;

import org.jobrunr.JobRunrException;
import org.jobrunr.utils.reflection.ReflectionUtils;

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
        super(StateName.FAILED);
        this.message = null;
        this.exceptionType = null;
        this.exceptionMessage = null;
        this.exceptionCauseType = null;
        this.exceptionCauseMessage = null;
        this.stackTrace = null;
        this.doNotRetry = false;
    }

    public FailedState(String message, Exception exception) {
        super(StateName.FAILED);
        this.message = message;
        this.exception = exception;
        this.exceptionType = exception.getClass().getName();
        this.exceptionMessage = exception.getMessage();
        this.exceptionCauseType = hasCause(exception) ? exception.getCause().getClass().getName() : null;
        this.exceptionCauseMessage = hasCause(exception) ? exception.getCause().getMessage() : null;
        this.stackTrace = getStackTraceAsString(exception);
        this.doNotRetry = isProblematicAndDoNotRetry(exception);
    }

    public FailedState(String message, String exceptionType, String exceptionMessage, String exceptionCauseType, String exceptionCauseMessage, String stackTrace, boolean doNotRetry) {
        super(StateName.FAILED);
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
