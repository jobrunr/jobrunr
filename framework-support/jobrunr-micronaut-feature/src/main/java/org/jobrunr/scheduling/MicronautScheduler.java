package org.jobrunr.scheduling;

import io.micronaut.inject.ExecutableMethod;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.micronaut.annotations.Recurring;
import org.jobrunr.scheduling.cron.CronExpression;

import java.time.ZoneId;

import static java.util.Collections.emptyList;

public class MicronautScheduler {

    private final JobScheduler jobScheduler;

    public MicronautScheduler(JobScheduler jobScheduler) {
        this.jobScheduler = jobScheduler;
    }

    public void schedule(ExecutableMethod<?, ?> method) {
        if (method.getTargetMethod().getParameterCount() > 0) {
            throw new IllegalStateException("Methods annotated with " + Recurring.class.getName() + " can not have parameters.");
        }

        String id = method.stringValue(Recurring.class, "id").orElse(null);
        JobDetails jobDetails = new JobDetails(method.getTargetMethod().getDeclaringClass().getName(), null, method.getTargetMethod().getName(), emptyList());
        CronExpression cron = method.stringValue(Recurring.class, "cron").map(CronExpression::create).orElseThrow(() -> new IllegalArgumentException("Cron attribute is required"));
        ZoneId zoneId = method.stringValue(Recurring.class, "zone").map(zone -> ZoneId.of(zone)).orElse(ZoneId.systemDefault());
        jobScheduler.scheduleRecurrently(id, jobDetails, cron, zoneId);
    }
}
