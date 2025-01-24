package org.jobrunr.scheduling;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.annotations.Recurring;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.jobrunr.utils.JobUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        // If running within a job context or method is not annotated with @Job, proceed as usual
        if (ThreadLocalJobContext.getJobContext() != null || !AnnotatedElementUtils.hasAnnotation(method, Job.class)) {
            return invocation.proceed();
        }

        // Extract job details
        String declaringClassName = method.getDeclaringClass().getName();
        String methodName = method.getName();
        JobDetails jobDetails = new JobDetails(declaringClassName, null, methodName, Stream.of(args).map(JobParameter::new).collect(Collectors.toList()));
        jobDetails.setCacheable(true);

        // Log and enqueue the job
        JobUtils.LOGGER.info("Enqueuing job for method - {} - with details - {}", methodName, jobDetails);
        jobScheduler.enqueue(null, jobDetails);

        // Return null since the method execution is delegated to the job scheduler
        return null;
    }
}
