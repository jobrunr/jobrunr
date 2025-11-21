package org.jobrunr.scheduling;

import io.micronaut.inject.ExecutableMethod;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.annotations.Recurring;
import org.jobrunr.jobs.context.JobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.jobrunr.jobs.RecurringJob.CreatedBy.ANNOTATION;

public class JobRunrRecurringJobScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobRunrRecurringJobScheduler.class);

    private final JobScheduler jobScheduler;

    public JobRunrRecurringJobScheduler(JobScheduler jobScheduler) {
        this.jobScheduler = jobScheduler;
    }

    public void schedule(ExecutableMethod<?, ?> method) {
        if (hasParametersOutsideOfJobContext(method.getTargetMethod())) {
            throw new IllegalStateException("Methods annotated with " + Recurring.class.getName() + " can only have zero parameters or a single parameter of type JobContext.");
        }

        String id = getId(method);
        String cron = getCron(method);
        String interval = getInterval(method);
        String scheduleExpression = ScheduleExpressionType.selectConfiguredScheduleExpression(cron, interval);

        if (Recurring.RECURRING_JOB_DISABLED.equals(scheduleExpression)) {
            if (id == null) {
                LOGGER.warn("You are trying to disable a recurring job using placeholders but did not define an id.");
            } else {
                jobScheduler.deleteRecurringJob(id);
            }
        } else {
            JobDetails jobDetails = getJobDetails(method);
            ZoneId zoneId = getZoneId(method);
            Schedule schedule = ScheduleExpressionType.createScheduleFromString(scheduleExpression);

            RecurringJob recurringJob = new RecurringJob(id, jobDetails, schedule, zoneId, ANNOTATION);
            jobScheduler.scheduleRecurrently(recurringJob);
        }
    }

    private boolean hasParametersOutsideOfJobContext(Method method) {
        if (method.getParameterCount() == 0) return false;
        else if (method.getParameterCount() > 1) return true;
        else return !method.getParameterTypes()[0].equals(JobContext.class);
    }

    private String getId(ExecutableMethod<?, ?> method) {
        return method.stringValue(Recurring.class, "id").orElse(null);
    }

    private String getCron(ExecutableMethod<?, ?> method) {
        return method.stringValue(Recurring.class, "cron").orElse(null);
    }

    private String getInterval(ExecutableMethod<?, ?> method) {
        return method.stringValue(Recurring.class, "interval").orElse(null);
    }

    private JobDetails getJobDetails(ExecutableMethod<?, ?> method) {
        List<JobParameter> jobParameters = new ArrayList<>();
        if (method.getTargetMethod().getParameterCount() == 1 && method.getTargetMethod().getParameterTypes()[0].equals(JobContext.class)) {
            jobParameters.add(JobParameter.JobContext);
        }
        final JobDetails jobDetails = new JobDetails(method.getTargetMethod().getDeclaringClass().getName(), null, method.getTargetMethod().getName(), jobParameters);
        jobDetails.setCacheable(true);
        return jobDetails;
    }

    private ZoneId getZoneId(ExecutableMethod<?, ?> method) {
        return method.stringValue(Recurring.class, "zoneId").map(ZoneId::of).orElse(ZoneId.systemDefault());
    }
}
