package org.jobrunr.jobs.details.lambdas;

import org.jobrunr.jobs.details.JavaJobDetailsBuilder;
import org.jobrunr.jobs.details.JobDetailsBuilder;
import org.jobrunr.jobs.lambdas.JobRunrJob;

import java.io.InputStream;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Modifier;

import static java.util.Arrays.stream;
import static org.jobrunr.JobRunrException.shouldNotHappenException;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.toFQClassName;
import static org.jobrunr.utils.StringUtils.substringAfter;

public class SerializedJobLambda {

    private final JobRunrJob jobRunrJob;
    protected final SerializedLambda serializedLambda;
    private final boolean functionalExpression;

    protected SerializedJobLambda(JobRunrJob jobRunrJob, SerializedLambda serializedLambda) {
        this.jobRunrJob = jobRunrJob;
        this.serializedLambda = serializedLambda;
        this.functionalExpression = isLambdaExpression() || isKotlinMethodReferenceBridge();
    }

    public boolean isFunctionalExpression() {
        return functionalExpression;
    }

    private boolean isLambdaExpression() {
        String methodName = serializedLambda.getImplMethodName();
        return methodName.startsWith("lambda$")
                || methodName.contains("$lambda-") // for Kotlin
                || methodName.contains("$lambda$");
    }

    // Kotlin 2.4 compiles a method reference to a private static bridge method, e.g. enclosingMethod$doWork
    private boolean isKotlinMethodReferenceBridge() {
        String methodName = serializedLambda.getImplMethodName();
        if (serializedLambda.getImplMethodKind() != MethodHandleInfo.REF_invokeStatic || !methodName.contains("$")) {
            return false;
        }
        try {
            Class<?> implClass = Class.forName(toFQClassName(serializedLambda.getImplClass()), false, jobRunrJob.getClass().getClassLoader());
            return JobLambdaUtils.isKotlinClass(implClass)
                    && stream(implClass.getDeclaredMethods()).anyMatch(method -> method.getName().equals(methodName) && Modifier.isPrivate(method.getModifiers()));
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public boolean matches(String methodName) {
        return isFunctionalExpression() && methodName.equals(serializedLambda.getImplMethodName());
    }

    public JobDetailsBuilder createJobDetailsBuilder(Object... params) {
        JobDetailsBuilder jobDetailsBuilder = new JavaJobDetailsBuilder(serializedLambda, params);
        if (!isFunctionalExpression()) {
            // Kotlin receivers need adaptation even when the reference originates in Java.
            adaptMethodReference(serializedLambda, jobDetailsBuilder);
        }
        return jobDetailsBuilder;
    }

    public InputStream getClassContainingLambdaAsInputStream() {
        String location = "/" + serializedLambda.getImplClass() + ".class";
        InputStream inputStream = jobRunrJob.getClass().getResourceAsStream(location);
        if (inputStream == null) {
            throw shouldNotHappenException("Could not find the class containing the lambda: " + location);
        }
        return inputStream;
    }

    private static void adaptMethodReference(SerializedLambda serializedLambda, JobDetailsBuilder jobDetailsBuilder) {
        if (serializedLambda.getCapturedArgCount() == 1
                && serializedLambda.getCapturedArg(0) != null
                && JobLambdaUtils.isKotlinClass(serializedLambda.getCapturedArg(0).getClass())) {
            jobDetailsBuilder.setClassName(serializedLambda.getCapturedArg(0).getClass().getName());
            String methodName = serializedLambda.getImplMethodName();
            jobDetailsBuilder.setMethodName(methodName.contains("$") ? substringAfter(methodName, "$") : methodName);
        }
    }
}
