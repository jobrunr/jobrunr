package org.jobrunr.server.runner;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.lambdas.JobWithoutIoc;

public class BackgroundJobWithoutIocRunner extends AbstractBackgroundJobRunner {

    @Override
    public boolean supports(Job job) {
        JobDetails jobDetails = job.getJobDetails();
        return JobWithoutIoc.class.getName().equals(jobDetails.getLambdaType()) && !jobDetails.getStaticFieldName().isPresent();
    }

    @Override
    protected BackgroundJobWorker getBackgroundJobWorker(Job job) {
        return new BackgroundJobWorker(job);
    }
}
