package org.jobrunr.jobs.details.lambdas;

import org.jobrunr.jobs.lambdas.JobRunrJob;

import java.lang.annotation.Annotation;
import java.lang.invoke.SerializedLambda;

import static java.util.Arrays.stream;
import static org.jobrunr.JobRunrException.shouldNotHappenException;
import static org.jobrunr.jobs.details.SerializedLambdaConverter.toSerializedLambda;

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

}
