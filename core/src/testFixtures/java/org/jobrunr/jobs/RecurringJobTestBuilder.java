package org.jobrunr.jobs;

import org.jobrunr.scheduling.cron.Cron;
import org.jobrunr.scheduling.cron.CronExpression;

import java.time.ZoneId;

import static org.jobrunr.jobs.JobDetailsTestBuilder.defaultJobDetails;

public class RecurringJobTestBuilder {

    private String id;
    private String name;
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

    public RecurringJobTestBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public RecurringJobTestBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public RecurringJobTestBuilder withJobDetails(JobDetailsTestBuilder jobDetailsBuilder) {
        this.jobDetails = jobDetailsBuilder.build();
        return this;
    }

    public RecurringJobTestBuilder withCronExpression(String cronExpression) {
        this.cronExpression = CronExpression.create(cronExpression);
        return this;
    }

    public RecurringJobTestBuilder withZoneId(ZoneId zoneId) {
        this.zoneId = zoneId;
        return this;
    }

    public RecurringJob build() {
        final RecurringJob recurringJob = new RecurringJob(id, jobDetails, cronExpression, zoneId);
        recurringJob.setJobName(name);
        return recurringJob;
    }


}