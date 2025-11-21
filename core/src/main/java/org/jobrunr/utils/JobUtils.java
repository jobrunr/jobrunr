package org.jobrunr.utils;

import org.jobrunr.jobs.AbstractJob;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.annotations.Recurring;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.scheduling.exceptions.JobClassNotFoundException;
import org.jobrunr.scheduling.exceptions.JobMethodNotFoundException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static org.jobrunr.utils.GraalVMUtils.isRunningInGraalVMNativeMode;
import static org.jobrunr.utils.StringUtils.substringAfterLast;
import static org.jobrunr.utils.StringUtils.substringBefore;
import static org.jobrunr.utils.StringUtils.substringBeforeLast;
import static org.jobrunr.utils.StringUtils.substringBetween;
import static org.jobrunr.utils.reflection.ReflectionUtils.classExists;
import static org.jobrunr.utils.reflection.ReflectionUtils.findMethod;
import static org.jobrunr.utils.reflection.ReflectionUtils.toClass;

public class JobUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobUtils.class);

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
        assertJobExists(getJobSignature(jobDetails));
    }

    public static void assertJobExists(String jobSignature) {
        String className = getClassNameFromJobSignature(jobSignature);
        String methodName = getMethodNameFromJobSignature(jobSignature);
        String[] jobParameterTypeNames = getParameterTypeNamesFromJobSignature(jobSignature);
        assertJobExists(className, methodName, jobParameterTypeNames);
    }

    public static void assertJobExists(String className, String methodName, String[] jobParameterTypeNames) {
        if (className.startsWith("java.") || className.startsWith("javax.")) return; // we assume that JDK classes don't change often
        if (!classExists(className)) {
            throw new JobClassNotFoundException(className, methodName, jobParameterTypeNames);
        }
        if (!jobExists(className, methodName, jobParameterTypeNames)) {
            throw new JobMethodNotFoundException(className, methodName, jobParameterTypeNames);
        }
    }

    public static boolean jobExists(String jobSignature) {
        if (jobSignature.startsWith("java.") || jobSignature.startsWith("javax.")) return true; // we assume that JDK classes don't change often
        String clazzName = getClassNameFromJobSignature(jobSignature);
        String methodName = getMethodNameFromJobSignature(jobSignature);
        String[] jobParameterTypeNames = getParameterTypeNamesFromJobSignature(jobSignature);
        return jobExists(clazzName, methodName, jobParameterTypeNames);

    }

    public static boolean jobExists(String clazzName, String methodName, String[] jobParameterTypeNames) {
        Optional<Method> optionalMethod = findMethod(clazzName, methodName, jobParameterTypeNames);
        if (optionalMethod.isPresent()) {
            Method method = optionalMethod.get();
            boolean isJobRequestHandlerWithWrongRunMethodArgument = Modifier.isAbstract(method.getModifiers()) && JobRequestHandler.class.isAssignableFrom(method.getDeclaringClass());
            return !isJobRequestHandlerWithWrongRunMethodArgument;
        }
        return false;
    }

    public static Optional<Job> getJobAnnotation(JobDetails jobDetails) {
        return getJobAnnotations(jobDetails)
                .filter(Job.class::isInstance)
                .map(Job.class::cast)
                .findFirst();
    }

    public static Optional<Recurring> getRecurringAnnotation(JobDetails jobDetails) {
        return getJobAnnotations(jobDetails)
                .filter(Recurring.class::isInstance)
                .map(Recurring.class::cast)
                .findFirst();
    }

    public static String getJobSignature(AbstractJob job) {
        return getJobSignature(job.getJobDetails());
    }

    public static String getJobSignature(JobDetails jobDetails) {
        String result = getJobClassAndMethodName(jobDetails);
        result += "(" + jobDetails.getJobParameters().stream().map(JobUtils::getJobParameterForSignature).collect(joining(", ")) + ")";
        return result;
    }

    private static Stream<Annotation> getJobAnnotations(JobDetails jobDetails) {
        if (jobDetails.getClassName().startsWith("java")) return Stream.empty();
        if (!classExists(jobDetails.getClassName())) {
            if (isRunningInGraalVMNativeMode()) {
                LOGGER.warn("Trying to find Job Annotations for '{}' but the class could not be found. The Job name and other properties like retries and labels will not be set on the Job. As you're running your application in GraalVM native mode, make sure that your job class is available in the native image. Normally, this is done automatically by JobRunr.", getJobSignature(jobDetails), new JobClassNotFoundException(jobDetails));
            } else {
                LOGGER.warn("Trying to find Job Annotations for '{}' but the class could not be found. The Job name and other properties like retries and labels will not be set on the Job.", getJobSignature(jobDetails), new JobClassNotFoundException(jobDetails));
            }
            return Stream.empty();
        }

        Method jobMethod = getJobMethod(jobDetails);
        return Stream.of(jobMethod.getDeclaredAnnotations());
    }

    public static String getClassNameFromJobSignature(String jobSignature) {
        return substringBeforeLast(getClassNameAndMethodFromJobSignature(jobSignature), ".");
    }

    public static String getMethodNameFromJobSignature(String jobSignature) {
        return substringAfterLast(getClassNameAndMethodFromJobSignature(jobSignature), ".");
    }

    public static String[] getParameterTypeNamesFromJobSignature(String jobSignature) {
        String jobParameterTypesAsString = substringBetween(jobSignature, "(", ")");
        if (jobParameterTypesAsString == null || jobParameterTypesAsString.replaceAll("\\s", "").isEmpty()) return new String[]{};
        return Stream.of(jobParameterTypesAsString.split(",")).map(String::trim).toArray(String[]::new);
    }

    private static String getClassNameAndMethodFromJobSignature(String jobSignature) {
        return substringBefore(jobSignature, "(");
    }

    private static String getJobParameterForSignature(JobParameter jobParameter) {
        return jobParameter.getObject() != null ? jobParameter.getObject().getClass().getName() : jobParameter.getClassName();
    }

    private static String getJobClassAndMethodName(JobDetails jobDetails) {
        String result = jobDetails.getClassName();
        Optional<String> staticFieldName = Optional.ofNullable(jobDetails.getStaticFieldName());
        if (staticFieldName.isPresent()) result += "." + staticFieldName.get();
        result += "." + jobDetails.getMethodName();
        return result;
    }

    private static @Nullable String getJobParameterValue(JobParameter jobParameter) {
        if (jobParameter.getClassName().equals(JobContext.class.getName())) {
            return JobContext.class.getSimpleName();
        } else if (jobParameter.getObject() == null) {
            return null;
        }
        return jobParameter.getObject().toString();
    }
}
