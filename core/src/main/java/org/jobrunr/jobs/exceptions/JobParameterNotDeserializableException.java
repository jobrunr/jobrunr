package org.jobrunr.jobs.exceptions;

public class JobParameterNotDeserializableException {

    private String className;
    private String exceptionMessage;

    protected JobParameterNotDeserializableException() {
        // used for deserialization, protected for JSONB
    }

    public JobParameterNotDeserializableException(Exception e) {
        this(e.getClass().getName(), e.getMessage());
    }

    public JobParameterNotDeserializableException(String className, String exceptionMessage) {
        this.className = className;
        this.exceptionMessage = exceptionMessage;
    }

    public String getClassName() {
        return className;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public String getStackTrace() {
        return "JobParameterNotDeserializableException: one of the JobParameters is not deserializable anymore"
                + "\nCaused by: " + className + ": " + exceptionMessage;
    }

    @Override
    public String toString() {
        return "JobParameterNotDeserializableException {" +
                "className='" + className + '\'' +
                ", exceptionMessage='" + exceptionMessage + '\'' +
                '}';
    }
}
