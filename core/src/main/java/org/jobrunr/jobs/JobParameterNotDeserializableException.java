package org.jobrunr.jobs;

public class JobParameterNotDeserializableException {

    private String className;
    private String exceptionMessage;

    protected JobParameterNotDeserializableException() {
        // used for deserialization, protected for JSONB
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

    @Override
    public String toString() {
        return "JobParameterNotDeserializableException {" +
                "className='" + className + '\'' +
                ", exceptionMessage='" + exceptionMessage + '\'' +
                '}';
    }
}
