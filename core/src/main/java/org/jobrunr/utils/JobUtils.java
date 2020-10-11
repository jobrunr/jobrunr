package org.jobrunr.utils;

import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.scheduling.exceptions.JobClassNotFoundException;
import org.jobrunr.scheduling.exceptions.JobMethodNotFoundException;
import org.jobrunr.utils.reflection.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static org.jobrunr.utils.reflection.ReflectionUtils.cast;
import static org.jobrunr.utils.reflection.ReflectionUtils.findMethod;
import static org.jobrunr.utils.reflection.ReflectionUtils.toClass;

public class JobUtils {

    private JobUtils() {
    }

    public static String getReadableNameFromJobDetails(JobDetails jobDetails) {
        String result = getJobClassAndMethodName(jobDetails);
        result += "(" + jobDetails.getJobParameters().stream().map(JobUtils::getJobParameterValue).collect(joining(",")) + ")";
        return result;
    }

    public static Class<?> getJobClass(JobDetails jobDetails) {
        try {
            return toClass(jobDetails.getClassName());
        } catch (IllegalArgumentException e) {
            throw new JobClassNotFoundException(jobDetails);
        }
    }

    public static Method getJobMethod(JobDetails jobDetails) {
        return getJobMethod(getJobClass(jobDetails), jobDetails);
    }

    public static Method getJobMethod(Class<?> jobClass, JobDetails jobDetails) {
        return findMethod(jobClass, jobDetails.getMethodName(), jobDetails.getJobParameterTypes())
                .orElseThrow(() -> new JobMethodNotFoundException(jobDetails));
    }

    public static void assertJobExists(JobDetails jobDetails) {
        if (getJobMethod(jobDetails) == null) throw new IllegalStateException("Job does not exist");
    }

    public static boolean jobExists(String jobSignature) {
        if (jobSignature.startsWith("java.") || jobSignature.startsWith("javax."))
            return true; // we assume that JDK classes don't change often

        try {
            String clazzAndMethod = getFQClassNameAndMethod(jobSignature);
            String clazzName = getFQClassName(clazzAndMethod);
            String method = getMethodName(clazzAndMethod);

            Class<Object> clazz = toClass(clazzName);
            Class<?>[] jobParameterTypes = getParameterTypes(jobSignature);
            return findMethod(clazz, method, jobParameterTypes).isPresent();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static Optional<Job> getJobAnnotation(JobDetails jobDetails) {
        return cast(getJobAnnotations(jobDetails).filter(jobAnnotation -> jobAnnotation.annotationType().equals(Job.class)).findFirst());
    }

    public static String getJobSignature(org.jobrunr.jobs.Job job) {
        return getJobSignature(job.getJobDetails());
    }

    public static String getJobSignature(JobDetails jobDetails) {
        String result = getJobClassAndMethodName(jobDetails);
        result += "(" + jobDetails.getJobParameters().stream().map(JobUtils::getJobParameterForSignature).collect(joining(",")) + ")";
        return result;
    }

    private static Stream<Annotation> getJobAnnotations(JobDetails jobDetails) {
        if (jobDetails.getClassName().startsWith("java")) return Stream.empty();

        Method jobMethod = getJobMethod(jobDetails);
        return Stream.of(jobMethod.getDeclaredAnnotations());
    }

    private static String getFQClassNameAndMethod(String jobSignature) {
        return jobSignature.substring(0, jobSignature.indexOf("("));
    }

    private static String getFQClassName(String clazzAndMethod) {
        return clazzAndMethod.substring(0, clazzAndMethod.lastIndexOf("."));
    }

    private static String getMethodName(String clazzAndMethod) {
        return clazzAndMethod.substring(clazzAndMethod.lastIndexOf(".") + 1);
    }

    private static Class<?>[] getParameterTypes(String jobSignature) {
        String jobParameterTypesAsString = jobSignature.substring(jobSignature.indexOf("(") + 1, jobSignature.length() - 1);

        if (jobParameterTypesAsString.replaceAll("\\s", "").isEmpty()) return new Class[]{};

        Class<?>[] jobParameterTypes = Arrays.stream(jobParameterTypesAsString.split(","))
                .map(ReflectionUtils::toClass)
                .toArray(Class[]::new);
        return jobParameterTypes;
    }

    private static String getJobParameterForSignature(JobParameter jobParameter) {
        return jobParameter.getObject() != null ? jobParameter.getObject().getClass().getName() : jobParameter.getClassName();
    }

    private static String getJobClassAndMethodName(JobDetails jobDetails) {
        String result = jobDetails.getClassName();
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
