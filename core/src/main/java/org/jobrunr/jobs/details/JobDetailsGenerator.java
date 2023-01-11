package org.jobrunr.jobs.details;

import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.lambdas.*;

public interface JobDetailsGenerator {

    JobDetails toJobDetails(JobLambda lambda);

    JobDetails toJobDetails(IocJobLambda<?> lambda);

    <T> JobDetails toJobDetails(T itemFromStream, JobLambdaFromStream<T> lambda);

    <S, T> JobDetails toJobDetails(T itemFromStream, IocJobLambdaFromStream<S, T> lambda);

    default JobDetails toJobDetails(JobRunrJob jobRunrJob) {
        if(jobRunrJob instanceof JobLambda) {
            return toJobDetails((JobLambda) jobRunrJob);
        } else if(jobRunrJob instanceof IocJobLambda) {
            return toJobDetails((IocJobLambda) jobRunrJob);
        }
        throw new IllegalArgumentException("The provided JobRunr job is not a valid JobRunr Job.");
    }
}
