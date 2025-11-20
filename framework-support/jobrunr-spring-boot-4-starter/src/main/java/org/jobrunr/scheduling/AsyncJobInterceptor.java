package org.jobrunr.scheduling;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.server.runner.ThreadLocalJobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.jobrunr.utils.JobUtils.getJobSignature;

public class AsyncJobInterceptor implements MethodInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncJobInterceptor.class);

    private final BeanFactory beanFactory;

    public AsyncJobInterceptor(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        // If method is not annotated with @Job or it is running the actual method, proceed as usual
        if (!AnnotatedElementUtils.hasAnnotation(invocation.getMethod(), Job.class) || isRunningActualJob(invocation)) {
            return invocation.proceed();
        }

        JobDetails jobDetails = getJobDetails(invocation);

        LOGGER.info("Enqueuing job via @AsyncJob {}", getJobSignature(jobDetails));
        beanFactory.getBean(JobScheduler.class).enqueue(null, jobDetails);

        // Return null since the method execution is delegated to the job scheduler and can run in a different JVM asynchronously
        return null;
    }

    private static boolean isRunningActualJob(MethodInvocation invocation) {
        JobContext jobContext = ThreadLocalJobContext.getJobContext();
        // if another @AsyncJob is created it must have a different class name due to limitations of Spring Proxies.
        return jobContext != null && jobContext.getJobSignature().startsWith(invocation.getMethod().getDeclaringClass().getName() + ".");
    }

    private static JobDetails getJobDetails(MethodInvocation methodInvocation) {
        Method method = methodInvocation.getMethod();
        String declaringClassName = method.getDeclaringClass().getName();
        String methodName = method.getName();
        JobDetails jobDetails = new JobDetails(declaringClassName, null, methodName, Stream.of(methodInvocation.getArguments()).map(JobParameter::new).collect(Collectors.toList()));
        jobDetails.setCacheable(true);
        return jobDetails;
    }
}
