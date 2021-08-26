package org.jobrunr.server.runner;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;

import java.lang.reflect.Modifier;

import static org.jobrunr.utils.JobUtils.getJobMethod;

public class BackgroundStaticJobWithoutIocRunner extends AbstractBackgroundJobRunner {

    @Override
    public boolean supports(Job job) {
        JobDetails jobDetails = job.getJobDetails();
        return !jobDetails.hasStaticFieldName() && Modifier.isStatic(getJobMethod(jobDetails).getModifiers());
    }

    @Override
    protected BackgroundJobWorker getBackgroundJobWorker(Job job) {
        return new StaticBackgroundJobWorker(job);
    }

    protected static class StaticBackgroundJobWorker extends BackgroundJobWorker {

        public StaticBackgroundJobWorker(Job job) {
            super(job);
        }

        @Override
        protected Object getJobToPerform(Class<?> jobToPerformClass) {
            return null;
        }
    }
}
