package org.jobrunr.scheduling.exceptions;

import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.JobParameterNotDeserializableException;

import java.util.stream.Stream;

public class JobNotFoundException extends RuntimeException {

    public JobNotFoundException(JobDetails jobDetails) {
        this(jobDetails.getClassName(),
                jobDetails.getMethodName(),
                jobDetails.getJobParameters().stream().map(JobNotFoundException::toJobParameterClassName).toArray(String[]::new),
                jobDetails.getJobParameters().stream().anyMatch(x -> x.getObject() instanceof JobParameterNotDeserializableException));
    }

    public JobNotFoundException(Class<?> clazz, String methodName, Class<?>[] parameterTypes) {
        this(clazz.getName(), methodName, Stream.of(parameterTypes).map(Class::getName).toArray(String[]::new), false);
    }

    public JobNotFoundException(String className, String methodName, String[] parameterTypes, boolean isJobParameterNotDeserializableException) {
        this(className + "." + methodName + "(" + String.join(",", parameterTypes) + ")" + (isJobParameterNotDeserializableException
                ? "\n\tcaused by: one of the JobParameters is not deserializable anymore"
                : ""));
    }

    public JobNotFoundException(String message) {
        super(message);
        this.setStackTrace(new StackTraceElement[0]);
    }

    private static String toJobParameterClassName(JobParameter jobParameter) {
        if(jobParameter.getObject() instanceof JobParameterNotDeserializableException) {
            return ((JobParameterNotDeserializableException)jobParameter.getObject()).getClassName();
        }
        return jobParameter.getClassName();
    }
}
