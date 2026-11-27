package org.jobrunr.jobs.details;

import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.lambdas.IocJobLambda;
import org.jobrunr.jobs.lambdas.IocJobLambdaFromStream;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.jobs.lambdas.JobLambdaFromStream;

public class JobDetailsClassFileApiGenerator implements JobDetailsGenerator {

    @Override
    public JobDetails toJobDetails(JobLambda lambda) {
        throw throwUnsupportedClassFileApi();
    }

    @Override
    public JobDetails toJobDetails(IocJobLambda<?> lambda) {
        throw throwUnsupportedClassFileApi();
    }

    @Override
    public <T> JobDetails toJobDetails(T itemFromStream, JobLambdaFromStream<T> lambda) {
        throw throwUnsupportedClassFileApi();
    }

    @Override
    public <S, T> JobDetails toJobDetails(T itemFromStream, IocJobLambdaFromStream<S, T> lambda) {
        throw throwUnsupportedClassFileApi();
    }

    private UnsupportedOperationException throwUnsupportedClassFileApi() {
        return new UnsupportedOperationException("You need Java 25 or higher to use JobDetailsClassFileApiGenerator.");
    }
}
