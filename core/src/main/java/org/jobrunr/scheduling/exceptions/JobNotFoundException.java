package org.jobrunr.scheduling.exceptions;

import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.exceptions.JobParameterNotDeserializableException;

import java.util.stream.Stream;

import static org.jobrunr.utils.StringUtils.isNotNullOrEmpty;

public class JobNotFoundException extends RuntimeException {

    public JobNotFoundException(JobDetails jobDetails) {
        this(jobDetails.getClassName(),
                jobDetails.getMethodName(),
                jobDetails.getJobParameters().stream().map(JobNotFoundException::toJobParameterClassName).toArray(String[]::new),
                jobDetails.getJobParameters().stream().filter(JobParameter::isNotDeserializable).map(JobParameter::getException).map(JobParameterNotDeserializableException::getStackTrace).findAny().orElse(null));
    }

    public JobNotFoundException(Class<?> clazz, String methodName, Class<?>[] parameterTypes) {
        this(clazz.getName(), methodName, Stream.of(parameterTypes).map(Class::getName).toArray(String[]::new), "");
    }

    public JobNotFoundException(String className, String methodName, String[] parameterTypes, String jobParameterNotDeserializableStackTrace) {
        this(className + "." + methodName + "(" + String.join(",", parameterTypes) + ")" + getJobParameterNotDeserializableStackTrace(jobParameterNotDeserializableStackTrace));
    }

    public JobNotFoundException(String message) {
        super(message);
        this.setStackTrace(new StackTraceElement[0]);
    }

    private static String toJobParameterClassName(JobParameter jobParameter) {
        return jobParameter.getClassName();
    }

    private static String getJobParameterNotDeserializableStackTrace(String jobParameterNotDeserializableStackTrace) {
        return isNotNullOrEmpty(jobParameterNotDeserializableStackTrace) ? "\nCaused by: " + jobParameterNotDeserializableStackTrace : "";
    }
}
