package org.jobrunr.server.runner;

import org.jobrunr.JobRunrException;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

public class BackgroundStaticJobWithoutIocRunner extends AbstractBackgroundJobRunner {

    @Override
    public boolean supports(Job job) {
        JobDetails jobDetails = job.getJobDetails();
        return jobDetails.getStaticFieldName().isPresent();
    }

    @Override
    protected BackgroundJobWorker getBackgroundJobWorker(Job job) {
        return new StaticFieldBackgroundJobWorker(job);
    }

    protected class StaticFieldBackgroundJobWorker extends BackgroundJobWorker {

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
            Optional<String> staticFieldName = jobDetails.getStaticFieldName();
            if (staticFieldName.isPresent()) {
                return jobContainingStaticFieldClass.getDeclaredField(staticFieldName.get());
            }
            throw JobRunrException.shouldNotHappenException("Expected a Job using a static field but it was not present");
        }
    }
}
