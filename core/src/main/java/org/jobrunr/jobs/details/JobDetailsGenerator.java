package org.jobrunr.jobs.details;

import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.lambdas.IocJobLambda;
import org.jobrunr.jobs.lambdas.IocJobLambdaFromStream;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.jobs.lambdas.JobLambdaFromStream;

public interface JobDetailsGenerator {

    JobDetails toJobDetails(JobLambda lambda);

    JobDetails toJobDetails(IocJobLambda<?> lambda);

    <T> JobDetails toJobDetails(T itemFromStream, JobLambdaFromStream<T> lambda);

    <S, T> JobDetails toJobDetails(T itemFromStream, IocJobLambdaFromStream<S, T> lambda);
}
