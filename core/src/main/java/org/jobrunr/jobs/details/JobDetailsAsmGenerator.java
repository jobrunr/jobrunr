package org.jobrunr.jobs.details;

import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.details.finders.KotlinAsmJobDetailsFinder;
import org.jobrunr.jobs.details.finders.SerializedLambdaAsmJobDetailsFinder;
import org.jobrunr.jobs.lambdas.IocJobLambda;
import org.jobrunr.jobs.lambdas.IocJobLambdaFromStream;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.jobs.lambdas.JobLambdaFromStream;
import org.jobrunr.jobs.lambdas.JobRunrJob;

import static org.jobrunr.jobs.details.lambdas.JobLambdaUtils.isKotlinClassBasedLambda;
import static org.jobrunr.jobs.details.lambdas.JobLambdaUtils.serializedLambda;

public class JobDetailsAsmGenerator implements JobDetailsGenerator {

    @Override
    public JobDetails toJobDetails(JobLambda lambda) {
        return isKotlinClassBasedLambda(lambda)
                ? kotlinJobDetails(lambda)
                : serializedLambdaJobDetails(lambda);
    }

    @Override
    public JobDetails toJobDetails(IocJobLambda<?> lambda) {
        return isKotlinClassBasedLambda(lambda)
                ? kotlinJobDetails(lambda, (Object) null)
                : serializedLambdaJobDetails(lambda);
    }

    @Override
    public <T> JobDetails toJobDetails(T itemFromStream, JobLambdaFromStream<T> lambda) {
        return isKotlinClassBasedLambda(lambda)
                ? kotlinJobDetails(lambda, itemFromStream)
                : serializedLambdaJobDetails(lambda, itemFromStream);
    }

    @Override
    public <S, T> JobDetails toJobDetails(T itemFromStream, IocJobLambdaFromStream<S, T> lambda) {
        // why null: it represents the item injected when we run the IocJobLambdaFromStream function
        return isKotlinClassBasedLambda(lambda)
                ? kotlinJobDetails(lambda, null, itemFromStream)
                : serializedLambdaJobDetails(lambda, null, itemFromStream);
    }

    private JobDetails kotlinJobDetails(JobRunrJob lambda, Object... params) {
        return new KotlinAsmJobDetailsFinder(lambda, params).getJobDetails();
    }

    private JobDetails serializedLambdaJobDetails(JobRunrJob lambda, Object... params) {
        return new SerializedLambdaAsmJobDetailsFinder(serializedLambda(lambda), params).getJobDetails();
    }
}
