package org.jobrunr.server.runner;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobContext;
import org.jobrunr.jobs.JobDetails;

import java.lang.reflect.Method;
import java.util.OptionalInt;
import java.util.stream.IntStream;

import static org.jobrunr.utils.reflection.ReflectionUtils.newInstance;

public abstract class AbstractBackgroundJobRunner implements BackgroundJobRunner {

    protected abstract BackgroundJobWorker getBackgroundJobWorker(Job job);

    public void run(Job job) throws Exception {
        getBackgroundJobWorker(job).run();
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

        protected Class<?> getJobToPerformClass() throws ClassNotFoundException {
            return Class.forName(jobDetails.getClassName());
        }

        protected Object getJobToPerform(Class<?> jobToPerformClass) {
            return newInstance(jobToPerformClass);
        }

        protected Method getJobMethodToPerform(Class<?> jobToPerformClass) throws NoSuchMethodException {
            return jobToPerformClass.getDeclaredMethod(jobDetails.getMethodName(), jobDetails.getJobParameterTypes());
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
