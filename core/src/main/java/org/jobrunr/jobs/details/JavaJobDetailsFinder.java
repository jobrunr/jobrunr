package org.jobrunr.jobs.details;

import org.jobrunr.jobs.lambdas.JobRunrJob;

import java.io.InputStream;
import java.lang.invoke.SerializedLambda;

import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.getJavaClassContainingLambdaAsInputStream;

public class JavaJobDetailsFinder extends AbstractJobDetailsFinder {

    private final JobRunrJob jobRunrJob;
    private final SerializedLambda serializedLambda;

    JavaJobDetailsFinder(JobRunrJob jobRunrJob, SerializedLambda serializedLambda, Object... params) {
        super(new JavaJobDetailsBuilder(serializedLambda, params));
        this.jobRunrJob = jobRunrJob;
        this.serializedLambda = serializedLambda;
        parse(getClassContainingLambdaAsInputStream());
    }

    @Override
    protected boolean isLambdaContainingJobDetails(String name) {
        return serializedLambda.getImplMethodName().startsWith("lambda$") && name.equals(serializedLambda.getImplMethodName());
    }

    @Override
    protected InputStream getClassContainingLambdaAsInputStream() {
        return getJavaClassContainingLambdaAsInputStream(jobRunrJob);
    }

}
