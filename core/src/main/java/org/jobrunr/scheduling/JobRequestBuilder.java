package org.jobrunr.scheduling;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.lambdas.JobRequest;

public class JobRequestBuilder extends AbstractJobBuilder<JobRequestBuilder, JobRequest> {

    private JobRequestBuilder() {
        // why: builder pattern
    }

    public static JobRequestBuilder aJob() {
        return new JobRequestBuilder();
    }

    Job build() {
        JobDetails jobDetails = new JobDetails(getJobDetails());
        return super.build(jobDetails);
    }

    @Override
    protected JobRequestBuilder self() {
        return this;
    }
}
