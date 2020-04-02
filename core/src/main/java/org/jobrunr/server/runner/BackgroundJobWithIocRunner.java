package org.jobrunr.server.runner;

import org.jobrunr.JobRunrException;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.lambdas.JobWithIoc;
import org.jobrunr.server.JobActivator;

public class BackgroundJobWithIocRunner extends AbstractBackgroundJobRunner {

    private final JobActivator jobActivator;

    public BackgroundJobWithIocRunner(JobActivator jobActivator) {
        this.jobActivator = jobActivator;
    }

    @Override
    public boolean supports(Job job) {
        JobDetails jobDetails = job.getJobDetails();
        boolean supportsJob = JobWithIoc.class.getName().equals(jobDetails.getLambdaType());
        if (supportsJob && jobActivator == null) throw new JobRunrException("A Job is enqueued without registering a proper JobActivator. Please register a JobActivator (See the documentation for more info.)");
        return supportsJob;
    }

    @Override
    protected BackgroundJobWorker getBackgroundJobWorker(Job job) {
        return new ConsumerBackgroundJobWorker(jobActivator, job);
    }

    protected static class ConsumerBackgroundJobWorker extends BackgroundJobWorker {

        private final JobActivator jobActivator;

        public ConsumerBackgroundJobWorker(JobActivator jobActivator, Job job) {
            super(job);
            this.jobActivator = jobActivator;
        }

        @Override
        protected Object getJobToPerform(Class<?> jobToPerformClass) {
            return jobActivator.activateJob(jobToPerformClass);
        }
    }
}
