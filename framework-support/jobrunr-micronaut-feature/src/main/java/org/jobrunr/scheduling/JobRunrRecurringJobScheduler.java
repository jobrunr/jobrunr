package org.jobrunr.scheduling;

import io.micronaut.inject.ExecutableMethod;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.micronaut.annotations.Recurring;
import org.jobrunr.scheduling.cron.CronExpression;
import org.jobrunr.scheduling.interval.Interval;
import org.jobrunr.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.time.ZoneId;

import static java.util.Collections.emptyList;
import static org.jobrunr.utils.StringUtils.isNotNullOrEmpty;

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

        if (StringUtils.isNullOrEmpty(cron) && StringUtils.isNullOrEmpty(interval))
            throw new IllegalArgumentException("Either cron or interval attribute is required.");
        if (isNotNullOrEmpty(cron) && isNotNullOrEmpty(interval))
            throw new IllegalArgumentException("Both cron and interval attribute provided. Only one is allowed.");

        if (Recurring.RECURRING_JOB_DISABLED.equals(cron) || Recurring.RECURRING_JOB_DISABLED.equals(interval)) {
            if (id == null) {
                LOGGER.warn("You are trying to disable a recurring job using placeholders but did not define an id.");
            } else {
                jobScheduler.delete(id);
            }
        } else {
            JobDetails jobDetails = getJobDetails(method);
            ZoneId zoneId = getZoneId(method);

            if (isNotNullOrEmpty(cron)) {
                jobScheduler.scheduleRecurrently(id, jobDetails, CronExpression.create(cron), zoneId);
            } else {
                jobScheduler.scheduleRecurrently(id, jobDetails, new Interval(interval), zoneId);
            }
        }
    }

    private boolean hasParametersOutsideOfJobContext(Method method) {
        if(method.getParameterCount() == 0) return false;
        else if(method.getParameterCount() > 1) return true;
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
        final JobDetails jobDetails = new JobDetails(method.getTargetMethod().getDeclaringClass().getName(), null, method.getTargetMethod().getName(), emptyList());
        jobDetails.setCacheable(true);
        return jobDetails;
    }

    private ZoneId getZoneId(ExecutableMethod<?, ?> method) {
        return method.stringValue(Recurring.class, "zoneId").map(ZoneId::of).orElse(ZoneId.systemDefault());
    }
}
