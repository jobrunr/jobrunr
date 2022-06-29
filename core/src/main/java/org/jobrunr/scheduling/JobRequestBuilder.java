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

    Job toJob() {
        Job job = createJob(new JobDetails(getJobDetails()));
        setJobName(job);
        setAmountOfRetries(job);
        return job;
    }

    @Override
    protected JobRequestBuilder self() {
        return this;
    }
}
