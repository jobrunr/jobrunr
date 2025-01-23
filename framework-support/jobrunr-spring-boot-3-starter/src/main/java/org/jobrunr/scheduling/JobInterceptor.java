package org.jobrunr.scheduling;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.jobrunr.utils.JobUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.List;

public class JobInterceptor implements MethodInterceptor {
    private final JobScheduler jobScheduler;

    public JobInterceptor(JobScheduler jobScheduler) {
        this.jobScheduler = jobScheduler;
    }

    @Nullable
    @Override
    public Object invoke(@Nonnull MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        Object[] args = invocation.getArguments();

        // If arguments are present, throw an exception (JobRunr limitation)
        if (args.length > 0) {
            throw new IllegalStateException("JobRunr does not support method arguments");
        }

        // If running within a job context or method is not annotated with @Job, proceed as usual
        if (ThreadLocalJobContext.getJobContext() != null || !method.isAnnotationPresent(Job.class)) {
            return invocation.proceed();
        }

        // Extract job details
        String declaringClassName = method.getDeclaringClass().getName();
        String methodName = method.getName();
        JobDetails jobDetails = new JobDetails(declaringClassName, null, methodName, List.of());
        jobDetails.setCacheable(true);

        // Log and enqueue the job
        JobUtils.LOGGER.info("Enqueuing job for method - {} - with details - {}", methodName, jobDetails);
        jobScheduler.enqueue(null, jobDetails);

        // Return null since the method execution is delegated to the job scheduler
        return null;
    }
}
