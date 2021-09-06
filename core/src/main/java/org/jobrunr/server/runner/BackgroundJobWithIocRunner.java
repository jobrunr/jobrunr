package org.jobrunr.server.runner;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.server.JobActivator;

import static org.jobrunr.utils.reflection.ReflectionUtils.toClass;

public class BackgroundJobWithIocRunner extends AbstractBackgroundJobRunner {

    private final JobActivator jobActivator;

    public BackgroundJobWithIocRunner(JobActivator jobActivator) {
        this.jobActivator = jobActivator;
    }

    @Override
    public boolean supports(Job job) {
        if (jobActivator == null) return false;
        JobDetails jobDetails = job.getJobDetails();
        return !jobDetails.hasStaticFieldName() && jobActivator.activateJob(toClass(jobDetails.getClassName())) != null;
    }

    @Override
    protected BackgroundJobWorker getBackgroundJobWorker(Job job) {
        return new BackgroundForIoCJobLambdaWorker(jobActivator, job);
    }

    protected static class BackgroundForIoCJobLambdaWorker extends BackgroundJobWorker {

        private final JobActivator jobActivator;

        public BackgroundForIoCJobLambdaWorker(JobActivator jobActivator, Job job) {
            super(job);
            this.jobActivator = jobActivator;
        }

        @Override
        protected Object getJobToPerform(Class<?> jobToPerformClass) {
            return jobActivator.activateJob(jobToPerformClass);
        }
    }
}
