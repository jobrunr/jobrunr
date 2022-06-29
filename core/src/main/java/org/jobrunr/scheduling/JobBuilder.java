package org.jobrunr.scheduling;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.details.JobDetailsGenerator;
import org.jobrunr.jobs.lambdas.JobLambda;

public class JobBuilder extends AbstractJobBuilder<JobBuilder, JobLambda> {

    private JobBuilder() {
        // why: builder pattern
    }

    public static JobBuilder aJob() {
        return new JobBuilder();
    }

    Job toJob(JobDetailsGenerator jobDetailsGenerator) {
        Job job = createJob(jobDetailsGenerator.toJobDetails(getJobDetails()));
        setJobName(job);
        setAmountOfRetries(job);
        return job;
    }

    @Override
    protected JobBuilder self() {
        return this;
    }
}
