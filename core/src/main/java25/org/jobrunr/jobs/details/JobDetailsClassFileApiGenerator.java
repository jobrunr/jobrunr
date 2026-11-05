package org.jobrunr.jobs.details;

import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.details.finders.KotlinClassFileApiJobDetailsFinder;
import org.jobrunr.jobs.details.finders.SerializedLambdaClassFileApiJobDetailsFinder;
import org.jobrunr.jobs.lambdas.IocJobLambda;
import org.jobrunr.jobs.lambdas.IocJobLambdaFromStream;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.jobs.lambdas.JobLambdaFromStream;
import org.jobrunr.jobs.lambdas.JobRunrJob;

import static org.jobrunr.jobs.details.lambdas.JobLambdaUtils.isKotlinClassBasedLambda;
import static org.jobrunr.jobs.details.lambdas.JobLambdaUtils.serializedLambda;

public class JobDetailsClassFileApiGenerator implements JobDetailsGenerator {

    @Override
    public JobDetails toJobDetails(JobLambda lambda) {
        return isKotlinClassBasedLambda(lambda)
                ? new KotlinClassFileApiJobDetailsFinder(lambda).getJobDetails()
                : serializedLambdaJobDetails(lambda);
    }

    @Override
    public JobDetails toJobDetails(IocJobLambda<?> lambda) {
        return isKotlinClassBasedLambda(lambda)
                ? new KotlinClassFileApiJobDetailsFinder(lambda, new Object()).getJobDetails()
                : serializedLambdaJobDetails(lambda);
    }

    @Override
    public <T> JobDetails toJobDetails(T itemFromStream, JobLambdaFromStream<T> lambda) {
        return isKotlinClassBasedLambda(lambda)
                ? new KotlinClassFileApiJobDetailsFinder(lambda, itemFromStream).getJobDetails()
                : serializedLambdaJobDetails(lambda, itemFromStream);
    }

    @Override
    public <S, T> JobDetails toJobDetails(T itemFromStream, IocJobLambdaFromStream<S, T> lambda) {
        // why new Object(): it represents the item injected when we run the IocJobLambdaFromStream function
        // why null: it represents the item injected when we run the IocJobLambdaFromStream function
        return isKotlinClassBasedLambda(lambda)
                ? new KotlinClassFileApiJobDetailsFinder(lambda, new Object(), itemFromStream).getJobDetails()
                : serializedLambdaJobDetails(lambda, null, itemFromStream);
    }

    private JobDetails serializedLambdaJobDetails(JobRunrJob lambda, Object... params) {
        return new SerializedLambdaClassFileApiJobDetailsFinder(serializedLambda(lambda), params).getJobDetails();
    }
}
