package org.jobrunr.utils;

import org.jobrunr.jobs.JobContext;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.annotations.Job;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static org.jobrunr.JobRunrException.problematicException;
import static org.jobrunr.utils.reflection.ReflectionUtils.cast;
import static org.jobrunr.utils.reflection.ReflectionUtils.findMethod;
import static org.jobrunr.utils.reflection.ReflectionUtils.noSuchMethodException;
import static org.jobrunr.utils.reflection.ReflectionUtils.toClass;

public class JobUtils {

    private JobUtils() {
    }

    public static Class getJobClass(JobDetails jobDetails) {
        return toClass(jobDetails.getClassName());
    }

    public static Method getJobMethod(JobDetails jobDetails) {
        return getJobMethod(getJobClass(jobDetails), jobDetails);
    }

    public static Method getJobMethod(Class<?> jobClass, JobDetails jobDetails) {
        return findMethod(jobClass, jobDetails.getMethodName(), jobDetails.getJobParameterTypes())
                .orElseThrow(() -> problematicException("JobRunr doesn't find the job method to perform", noSuchMethodException(jobClass.getName(), jobDetails.getMethodName(), jobDetails.getJobParameterTypes())));
    }

    public static <T extends Annotation> Optional<T> getJobAnnotation(JobDetails jobDetails) {
        return cast(getJobAnnotations(jobDetails).filter(jobAnnotation -> jobAnnotation.annotationType().equals(Job.class)).findFirst());
    }

    private static Stream<Annotation> getJobAnnotations(JobDetails jobDetails) {
        if (jobDetails.getClassName().startsWith("java")) return Stream.empty();

        Method jobMethod = getJobMethod(jobDetails);
        return Stream.of(jobMethod.getDeclaredAnnotations());
    }

    public static String getJobSignature(org.jobrunr.jobs.Job job) {
        return getJobSignature(job.getJobDetails());
    }

    public static String getJobSignature(JobDetails jobDetails) {
        String result = getJobClassAndMethodName(jobDetails);
        result += "(" + jobDetails.getJobParameters().stream().map(JobUtils::getJobParameterForSignature).collect(joining(",")) + ")";
        return result;
    }

    private static String getJobParameterForSignature(JobParameter jobParameter) {
        return jobParameter.getObject() != null ? jobParameter.getObject().getClass().getSimpleName() : jobParameter.getClassName();
    }

    public static String getReadableNameFromJobDetails(JobDetails jobDetails) {
        String result = getJobClassAndMethodName(jobDetails);
        result += "(" + jobDetails.getJobParameters().stream().map(JobUtils::getJobParameterValue).collect(joining(",")) + ")";
        return result;

    }

    private static String getJobClassAndMethodName(JobDetails jobDetails) {
        String result = jobDetails.getClassName().substring(jobDetails.getClassName().lastIndexOf('.') + 1);
        Optional<String> staticFieldName = jobDetails.getStaticFieldName();
        if (staticFieldName.isPresent()) result += "." + staticFieldName.get();
        result += "." + jobDetails.getMethodName();
        return result;
    }

    private static String getJobParameterValue(JobParameter jobParameter) {
        if (jobParameter.getClassName().equals(JobContext.class.getName())) {
            return JobContext.class.getSimpleName();
        }
        return jobParameter.getObject().toString();
    }
}
