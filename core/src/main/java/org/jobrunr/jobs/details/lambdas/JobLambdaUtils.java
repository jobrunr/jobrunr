package org.jobrunr.jobs.details.lambdas;

import org.jobrunr.jobs.details.JobDetailsBuilder;
import org.jobrunr.jobs.lambdas.JobRunrJob;

import java.lang.annotation.Annotation;
import java.lang.invoke.SerializedLambda;

import static java.util.Arrays.stream;
import static org.jobrunr.JobRunrException.shouldNotHappenException;
import static org.jobrunr.jobs.details.SerializedLambdaConverter.toSerializedLambda;
import static org.jobrunr.utils.StringUtils.substringAfter;

public final class JobLambdaUtils {

    private JobLambdaUtils() {
    }

    public static boolean isKotlinClassBasedLambda(Object lambda) {
        return isKotlinClass(lambda.getClass());
    }

    static boolean isKotlinClass(Class<?> type) {
        return stream(type.getDeclaredAnnotations())
                .map(Annotation::annotationType)
                .anyMatch(annotationType -> annotationType.getName().equals("kotlin.Metadata"));
    }

    public static SerializedJobLambda serializedLambda(JobRunrJob jobRunrJob) {
        SerializedLambda serializedLambda = toSerializedLambda(jobRunrJob);
        try {
            Class<?> capturingClass = Class.forName(serializedLambda.getCapturingClass().replace('/', '.'), false, jobRunrJob.getClass().getClassLoader());
            return isKotlinClass(capturingClass)
                    ? new KotlinSerializedLambda(jobRunrJob, serializedLambda)
                    : new JavaSerializedLambda(jobRunrJob, serializedLambda);
        } catch (ClassNotFoundException e) {
            throw shouldNotHappenException(e);
        }
    }

    public static void adaptMethodReference(SerializedLambda serializedLambda, JobDetailsBuilder jobDetailsBuilder) {
        if (serializedLambda.getCapturedArgCount() == 1
                && serializedLambda.getCapturedArg(0) != null
                && isKotlinClass(serializedLambda.getCapturedArg(0).getClass())) {
            jobDetailsBuilder.setClassName(serializedLambda.getCapturedArg(0).getClass().getName());
            String methodName = serializedLambda.getImplMethodName();
            jobDetailsBuilder.setMethodName(methodName.contains("$") ? substringAfter(methodName, "$") : methodName);
        }
    }

}
