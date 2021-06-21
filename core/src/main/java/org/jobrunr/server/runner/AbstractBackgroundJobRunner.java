package org.jobrunr.server.runner;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.utils.JobUtils;

import java.lang.reflect.Method;
import java.util.OptionalInt;
import java.util.stream.IntStream;

import static org.jobrunr.utils.reflection.ReflectionUtils.newInstance;

public abstract class AbstractBackgroundJobRunner implements BackgroundJobRunner {

    protected abstract BackgroundJobWorker getBackgroundJobWorker(Job job);

    public void run(Job job) throws Exception {
        getBackgroundJobWorker(job).run();
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
    }

    protected static class BackgroundJobWorker {

        private final Job job;
        protected final JobDetails jobDetails;

        public BackgroundJobWorker(Job job) {
            this.job = job;
            this.jobDetails = job.getJobDetails();
        }

        public void run() throws Exception {
            Class<?> jobToPerformClass = getJobToPerformClass();
            Object jobToPerform = getJobToPerform(jobToPerformClass);
            Method jobMethodToPerform = getJobMethodToPerform(jobToPerformClass);
            invokeJobMethod(jobToPerform, jobMethodToPerform);
        }

        protected Class<?> getJobToPerformClass() {
            return JobUtils.getJobClass(jobDetails);
        }

        protected Object getJobToPerform(Class<?> jobToPerformClass) {
            return newInstance(jobToPerformClass);
        }

        protected Method getJobMethodToPerform(Class<?> jobToPerformClass) {
            return JobUtils.getJobMethod(jobToPerformClass, jobDetails);
        }

        protected void invokeJobMethod(Object jobToPerform, Method jobMethodToPerform) throws Exception {
            Object[] jobParameterValues = jobDetails.getJobParameterValues();

            OptionalInt indexOfJobContext = IntStream.range(0, jobDetails.getJobParameters().size())
                    .filter(i -> jobDetails.getJobParameters().get(i).getClassName().equals(JobContext.class.getName()))
                    .findFirst();
            indexOfJobContext.ifPresent(index -> jobParameterValues[index] = new RunnerJobContext(job));

            jobMethodToPerform.invoke(jobToPerform, jobParameterValues);
        }
    }
}
