package org.jobrunr.jobs;

public class JobParameter {

    public static final JobParameter JobContext = new JobParameter(JobContext.class, null);

    private String className;
    private Object object;

    private JobParameter() {
        // used for deserialization
    }

    public JobParameter(Class<?> clazz, Object object) {
        this(clazz.getName(), object);
    }

    public JobParameter(String className, Object object) {
        this.className = className;
        this.object = object;
    }

    public String getClassName() {
        return className;
    }

    public Object getObject() {
        return object;
    }

}
