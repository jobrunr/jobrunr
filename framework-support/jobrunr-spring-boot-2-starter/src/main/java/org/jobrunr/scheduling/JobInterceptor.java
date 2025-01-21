package org.jobrunr.scheduling;

import net.bytebuddy.implementation.bind.annotation.*;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.server.runner.ThreadLocalJobContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.jobrunr.utils.JobUtils.LOGGER;

public class JobInterceptor {
    JobScheduler jobScheduler;

    public JobInterceptor(JobScheduler jobScheduler) {
        this.jobScheduler = jobScheduler;
    }

    @RuntimeType
    public Object intercept(
            @This Object self,
            @Origin Method method,
            @AllArguments Object[] args,
            @SuperMethod(nullIfImpossible = true) Method superMethod,
            @Empty Object defaultValue) throws Throwable {

        if(args.length > 0) {
            throw new RuntimeException("JobRunr does not support method arguments");
        }

        if (ThreadLocalJobContext.getJobContext() != null || !method.isAnnotationPresent(Job.class)) {
            try {
                return superMethod.invoke(self, args);
            } catch (InvocationTargetException | IllegalAccessException e) {
                LOGGER.error("Exception invoking method: " + method.getName(), e.getCause());
            }
        }

        String declaringClassName = method.getDeclaringClass().getName();
        String methodName = method.getName();
        List<JobParameter> jobParameters = Arrays.stream(args).map(JobParameter::new).collect(toList());
        JobDetails jobDetails = new JobDetails(declaringClassName, null, methodName, jobParameters);
        jobDetails.setCacheable(true);
        LOGGER.info("Enqueuing job for method - {} - with details - {}", methodName, jobDetails);
        jobScheduler.enqueue(null, jobDetails);
        return null;
    }
}
