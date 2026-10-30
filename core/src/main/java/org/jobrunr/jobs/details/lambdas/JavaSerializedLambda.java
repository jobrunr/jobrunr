package org.jobrunr.jobs.details.lambdas;

import org.jobrunr.jobs.lambdas.JobRunrJob;

import java.lang.invoke.SerializedLambda;

final class JavaSerializedLambda extends SerializedJobLambda {

    JavaSerializedLambda(JobRunrJob jobRunrJob, SerializedLambda serializedLambda) {
        super(jobRunrJob, serializedLambda);
    }

    @Override
    public boolean isLambdaBody() {
        return serializedLambda.getImplMethodName().startsWith("lambda$");
    }
}
