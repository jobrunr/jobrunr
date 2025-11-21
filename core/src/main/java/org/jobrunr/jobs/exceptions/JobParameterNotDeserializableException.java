package org.jobrunr.jobs.exceptions;

import org.jspecify.annotations.Nullable;

public class JobParameterNotDeserializableException {

    private final String objectClassName;
    private final String exceptionClassName;
    private final @Nullable String exceptionMessage;

    public JobParameterNotDeserializableException(String objectClassName, Exception e) {
        this(objectClassName, e.getClass().getName(), e.getMessage());
    }

    public JobParameterNotDeserializableException(String objectClassName, String exceptionClassName, @Nullable String exceptionMessage) {
        this.objectClassName = objectClassName;
        this.exceptionClassName = exceptionClassName;
        this.exceptionMessage = exceptionMessage;
    }

    public String getObjectClassName() {
        return objectClassName;
    }

    public String getExceptionClassName() {
        return exceptionClassName;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public String getStackTrace() {
        return "JobParameterNotDeserializableException: one of the JobParameters of type '" + objectClassName + "' is not deserializable anymore"
                + "\nCaused by: " + exceptionClassName + ": " + exceptionMessage;
    }

    @Override
    public String toString() {
        return "JobParameterNotDeserializableException {" +
                "objectClassName='" + objectClassName + '\'' +
                ", exceptionClassName='" + exceptionClassName + '\'' +
                ", exceptionMessage='" + exceptionMessage + '\'' +
                '}';
    }
}
