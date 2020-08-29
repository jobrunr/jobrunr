package org.jobrunr.jobs.details;

import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.lambdas.IocJobLambdaFromStream;
import org.jobrunr.jobs.lambdas.JobLambdaFromStream;
import org.jobrunr.jobs.lambdas.JobRunrJob;

public interface JobDetailsGenerator {

    <T extends JobRunrJob> JobDetails toJobDetails(T lambda);

    <T> JobDetails toJobDetails(T itemFromStream, JobLambdaFromStream<T> lambda);

    <S, T> JobDetails toJobDetails(T itemFromStream, IocJobLambdaFromStream<S, T> lambda);
}
