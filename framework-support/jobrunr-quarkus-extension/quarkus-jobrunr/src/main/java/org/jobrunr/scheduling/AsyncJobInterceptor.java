package org.jobrunr.scheduling;

import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.annotations.AsyncJob;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.jobrunr.utils.JobUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.jobrunr.utils.JobUtils.getJobSignature;

@AsyncJob
@Interceptor
public class AsyncJobInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncJobInterceptor.class);

    @Inject
    JobScheduler jobScheduler;

    @AroundInvoke
    public Object intercept(InvocationContext ctx) throws Exception {
        JobDetails jobDetails = getJobDetails(ctx);
        var method = ctx.getMethod();

        if (!method.isAnnotationPresent(Job.class) || isRunningActualJob(jobDetails)) return ctx.proceed();

        LOGGER.info("Enqueuing job via @AsyncJob {}", getJobSignature(jobDetails));
        jobScheduler.enqueue(null, jobDetails);
        return null;
    }

    private JobDetails getJobDetails(InvocationContext ctx) {
        String declaringClassName = ctx.getMethod().getDeclaringClass().getName();
        String methodName = ctx.getMethod().getName();
        JobDetails jobDetails = new JobDetails(declaringClassName, null, methodName, Stream.of(ctx.getParameters()).map(JobParameter::new).collect(Collectors.toList()));
        jobDetails.setCacheable(true);
        return jobDetails;
    }

    private static boolean isRunningActualJob(JobDetails jobDetails) {
        JobContext jobContext = ThreadLocalJobContext.getJobContext();
        return jobContext != null && jobContext.getJobSignature().equals(JobUtils.getJobSignature(jobDetails));
    }

}
