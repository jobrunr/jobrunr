package org.jobrunr.jobs;

import org.jobrunr.jobs.exceptions.JobParameterNotDeserializableException;

public class JobParameter {

    public static final JobParameter JobContext = new JobParameter(org.jobrunr.jobs.context.JobContext.class);

    private String className;
    private String actualClassName;
    private Object object;
    private JobParameterNotDeserializableException exception;

    private JobParameter() {
        // used for deserialization
    }

    private JobParameter(Class<?> clazz) {
        this(clazz.getName(), null);
    }

    public JobParameter(Class<?> clazz, Object object) {
        this(clazz.getName(), object);
    }

    public JobParameter(Object object) {
        this(object.getClass().getName(), object);
    }

    public JobParameter(String className, Object object) {
        this(className, isNotNullNorAnEnum(object) ? object.getClass().getName() : className, object);
    }

    public JobParameter(String className, String actualClassName, Object object) {
        this(className, actualClassName, object, null);
    }

    public JobParameter(String className, String actualClassName, Object object, JobParameterNotDeserializableException exception) {
        this.className = className;
        this.actualClassName = actualClassName;
        this.object = object;
        this.exception = exception;
    }

    /**
     * Represents the class name expected by the job method (e.g. an object or an interface)
     *
     * @return the class name expected by the job method (e.g. an object or an interface)
     */
    public String getClassName() {
        return className;
    }

    /**
     * Represents the actual class name of the job parameter (e.g. an object), this will never be an interface
     *
     * @return the actual class name of the job parameter (e.g. an object), this will never be an interface
     */
    public String getActualClassName() {
        return actualClassName;
    }

    /**
     * The actual job parameter
     *
     * @return the actual job parameter
     */
    public Object getObject() {
        return object;
    }

    public JobParameterNotDeserializableException getException() {
        return exception;
    }

    public boolean isNotDeserializable() {
        return exception != null;
    }

    protected static boolean isNotNullNorAnEnum(Object object) {
        return object != null && !(object instanceof Enum);
    }
}
