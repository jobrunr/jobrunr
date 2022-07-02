package org.jobrunr.scheduling;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.details.JobDetailsGenerator;
import org.jobrunr.jobs.lambdas.JobLambda;

public class JobBuilder extends AbstractJobBuilder<JobBuilder, JobLambda> {

    private JobBuilder() {
        // why: builder pattern
    }

    public static JobBuilder aJob() {
        return new JobBuilder();
    }

    Job build(JobDetailsGenerator jobDetailsGenerator) {
        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(getJobDetails());
        return super.build(jobDetails);
    }

    @Override
    protected JobBuilder self() {
        return this;
    }
}
