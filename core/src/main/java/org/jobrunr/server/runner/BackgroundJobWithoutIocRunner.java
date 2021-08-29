package org.jobrunr.server.runner;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.context.JobContext;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.jobrunr.utils.reflection.ReflectionUtils.findField;
import static org.jobrunr.utils.reflection.ReflectionUtils.hasDefaultNoArgConstructor;
import static org.jobrunr.utils.reflection.ReflectionUtils.setFieldUsingAutoboxing;

public class BackgroundJobWithoutIocRunner extends AbstractBackgroundJobRunner {

    @Override
    public boolean supports(Job job) {
        JobDetails jobDetails = job.getJobDetails();
        return !jobDetails.hasStaticFieldName() && hasDefaultNoArgConstructor(jobDetails.getClassName());
    }

    @Override
    protected BackgroundJobWorker getBackgroundJobWorker(Job job) {
        return new BackgroundForJobLambdaWorker(job);
    }

    protected static class BackgroundForJobLambdaWorker extends BackgroundJobWorker {

        public BackgroundForJobLambdaWorker(Job job) {
            super(job);
        }

        @Override
        protected Object getJobToPerform(Class<?> jobToPerformClass) {
            final Object object = super.getJobToPerform(jobToPerformClass);
            final Optional<Field> jobContextOptional = findField(jobToPerformClass, f -> f.getType().equals(JobContext.class));
            jobContextOptional.ifPresent(jobContext -> setFieldUsingAutoboxing(jobContext, object, getRunnerJobContext()));
            return object;
        }
    }
}
