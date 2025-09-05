package org.jobrunr.scheduling;

import io.micronaut.aop.InterceptorBean;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
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

@Singleton
@InterceptorBean(AsyncJob.class)
public class AsyncJobInterceptor implements MethodInterceptor<Object, Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncJobInterceptor.class);

    @Inject
    private JobScheduler jobScheduler;

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> ctx) {
        JobDetails jobDetails = getJobDetails(ctx);
        var method = ctx.getExecutableMethod();

        if (!method.isAnnotationPresent(Job.class) || isRunningActualJob(jobDetails)) return ctx.proceed();

        LOGGER.info("Enqueuing job via @AsyncJob {}", getJobSignature(jobDetails));
        jobScheduler.enqueue(null, jobDetails);
        return null;
    }

    private JobDetails getJobDetails(MethodInvocationContext<Object, Object> ctx) {
        String declaringClassName = ctx.getDeclaringType().getName();
        String methodName = ctx.getExecutableMethod().getName();
        JobDetails jobDetails = new JobDetails(declaringClassName, null, methodName, Stream.of(ctx.getParameterValues()).map(JobParameter::new).collect(Collectors.toList()));
        jobDetails.setCacheable(true);
        return jobDetails;
    }

    private static boolean isRunningActualJob(JobDetails jobDetails) {
        JobContext jobContext = ThreadLocalJobContext.getJobContext();
        return jobContext != null && jobContext.getJobSignature().equals(JobUtils.getJobSignature(jobDetails));
    }
}
