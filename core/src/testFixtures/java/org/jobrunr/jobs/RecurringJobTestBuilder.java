package org.jobrunr.jobs;

import org.jobrunr.scheduling.cron.Cron;
import org.jobrunr.scheduling.cron.CronExpression;

import java.time.ZoneId;

import static org.jobrunr.jobs.JobDetailsTestBuilder.defaultJobDetails;

public class RecurringJobTestBuilder {

    private String id;
    private JobDetails jobDetails;
    private CronExpression cronExpression;
    private ZoneId zoneId;

    private RecurringJobTestBuilder() {

    }

    public static RecurringJobTestBuilder aRecurringJob() {
        return new RecurringJobTestBuilder();
    }

    public static RecurringJobTestBuilder aDefaultRecurringJob() {
        return aRecurringJob()
                .withId("anId")
                .withJobDetails(defaultJobDetails())
                .withCronExpression(Cron.daily())
                .withZoneId(ZoneId.systemDefault());
    }

    private RecurringJobTestBuilder withId(String id) {
        this.id = id;
        return this;
    }

    private RecurringJobTestBuilder withJobDetails(JobDetailsTestBuilder jobDetailsBuilder) {
        this.jobDetails = jobDetailsBuilder.build();
        return this;
    }

    private RecurringJobTestBuilder withCronExpression(String cronExpression) {
        this.cronExpression = CronExpression.create(cronExpression);
        return this;
    }

    private RecurringJobTestBuilder withZoneId(ZoneId zoneId) {
        this.zoneId = zoneId;
        return this;
    }

    public RecurringJob build() {
        return new RecurringJob(id, jobDetails, cronExpression, zoneId);
    }
}