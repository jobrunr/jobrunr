package org.jobrunr.scheduling;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.scheduling.cron.CronExpression;

import java.time.ZoneId;

@Recorder
public class JobRunrRecorder {

    public void schedule(BeanContainer container, String id, JobDetails jobDetails, CronExpression cronExpression, ZoneId zoneId) {
        JobScheduler scheduler = container.instance(JobScheduler.class);
        scheduler.scheduleRecurrently(id, jobDetails, cronExpression, zoneId);
    }

}
