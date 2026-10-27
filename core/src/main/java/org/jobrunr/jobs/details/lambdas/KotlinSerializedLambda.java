package org.jobrunr.jobs.details.lambdas;

import org.jobrunr.jobs.lambdas.JobRunrJob;

import java.lang.invoke.SerializedLambda;

final class KotlinSerializedLambda extends SerializedJobLambda {

    KotlinSerializedLambda(JobRunrJob jobRunrJob, SerializedLambda serializedLambda) {
        super(jobRunrJob, serializedLambda);
    }

    @Override
    public boolean isLambdaBody() {
        String methodName = serializedLambda.getImplMethodName();
        return methodName.startsWith("lambda$")
                || methodName.contains("$lambda-")
                || methodName.contains("$lambda$");
    }
}
