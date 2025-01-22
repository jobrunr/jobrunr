package org.jobrunr.scheduling;

import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.jobrunr.utils.JobUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class JobInterceptor {
    private final JobScheduler jobScheduler;

    public JobInterceptor(JobScheduler jobScheduler) {
        this.jobScheduler = jobScheduler;
    }

    public Object intercept(org.aopalliance.intercept.MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        Object[] args = invocation.getArguments();

        // If arguments are present, throw an exception (JobRunr limitation)
        if (args.length > 0) {
            throw new RuntimeException("JobRunr does not support method arguments");
        }

        // If running within a job context or method is not annotated with @Job, proceed as usual
        if (ThreadLocalJobContext.getJobContext() != null || !method.isAnnotationPresent(Job.class)) {
            return invocation.proceed();
        }

        // Extract job details
        String declaringClassName = method.getDeclaringClass().getName();
        String methodName = method.getName();
        List<JobParameter> jobParameters = Arrays.stream(args)
                .map(JobParameter::new)
                .collect(Collectors.toList());
        JobDetails jobDetails = new JobDetails(declaringClassName, null, methodName, jobParameters);
        jobDetails.setCacheable(true);

        // Log and enqueue the job
        JobUtils.LOGGER.info("Enqueuing job for method - {} - with details - {}", methodName, jobDetails);
        jobScheduler.enqueue(null, jobDetails);

        // Return null since the method execution is delegated to the job scheduler
        return null;
    }
}
