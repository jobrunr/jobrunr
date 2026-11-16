package org.jobrunr.jobs.details.lambdas;

import org.jobrunr.jobs.lambdas.JobRunrJob;

import java.lang.annotation.Annotation;
import java.lang.invoke.SerializedLambda;

import static java.util.Arrays.stream;
import static org.jobrunr.jobs.details.SerializedLambdaConverter.toSerializedLambda;

public final class JobLambdaUtils {

    private static final ClassValue<Boolean> IS_KOTLIN_CLASS_CACHE = new ClassValue<Boolean>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            return stream(type.getDeclaredAnnotations())
                    .map(Annotation::annotationType)
                    .anyMatch(annotationType -> annotationType.getName().equals("kotlin.Metadata"));
        }
    };

    private JobLambdaUtils() {
    }

    public static boolean isKotlinClassBasedLambda(Object lambda) {
        return isKotlinClass(lambda.getClass());
    }

    static boolean isKotlinClass(Class<?> type) {
        return IS_KOTLIN_CLASS_CACHE.get(type);
    }

    public static SerializedJobLambda serializedLambda(JobRunrJob jobRunrJob) {
        SerializedLambda serializedLambda = toSerializedLambda(jobRunrJob);
        return new SerializedJobLambda(jobRunrJob, serializedLambda);
    }

}
