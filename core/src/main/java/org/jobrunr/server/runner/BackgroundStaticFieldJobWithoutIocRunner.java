package org.jobrunr.server.runner;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class BackgroundStaticFieldJobWithoutIocRunner extends AbstractBackgroundJobRunner {

    @Override
    public boolean supports(Job job) {
        JobDetails jobDetails = job.getJobDetails();
        return jobDetails.hasStaticFieldName();
    }

    @Override
    protected BackgroundJobWorker getBackgroundJobWorker(Job job) {
        return new StaticFieldBackgroundJobWorker(job);
    }

    protected static class StaticFieldBackgroundJobWorker extends BackgroundJobWorker {

        public StaticFieldBackgroundJobWorker(Job job) {
            super(job);
        }

        @Override
        public void run() throws Exception {
            Class<?> jobContainingStaticFieldClass = getJobToPerformClass();
            Field jobField = getStaticFieldOfJobToPerformClass(jobContainingStaticFieldClass);
            Class<?> jobToPerformClass = jobField.getType();
            Method methodToPerform = getJobMethodToPerform(jobToPerformClass);
            invokeJobMethod(jobField.get(null), methodToPerform);
        }

        private Field getStaticFieldOfJobToPerformClass(Class<?> jobContainingStaticFieldClass) throws NoSuchFieldException {
            String staticFieldName = jobDetails.getStaticFieldName();
            return jobContainingStaticFieldClass.getDeclaredField(staticFieldName);
        }
    }
}
