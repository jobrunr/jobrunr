package org.jobrunr.jobs.details.lambdas;

import org.jobrunr.jobs.details.JavaJobDetailsBuilder;
import org.jobrunr.jobs.details.JobDetailsBuilder;
import org.jobrunr.jobs.lambdas.JobRunrJob;

import java.io.InputStream;
import java.lang.invoke.SerializedLambda;

import static org.jobrunr.JobRunrException.shouldNotHappenException;
import static org.jobrunr.utils.StringUtils.substringAfter;

public abstract class SerializedJobLambda {

    protected final SerializedLambda serializedLambda;
    private final JobRunrJob jobRunrJob;

    protected SerializedJobLambda(JobRunrJob jobRunrJob, SerializedLambda serializedLambda) {
        this.jobRunrJob = jobRunrJob;
        this.serializedLambda = serializedLambda;
    }

    public abstract boolean isLambdaBody();

    public boolean matches(String methodName) {
        return isLambdaBody() && methodName.equals(serializedLambda.getImplMethodName());
    }

    public JobDetailsBuilder createJobDetailsBuilder(Object... params) {
        JobDetailsBuilder jobDetailsBuilder = new JavaJobDetailsBuilder(serializedLambda, params);
        if (!isLambdaBody()) {
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
