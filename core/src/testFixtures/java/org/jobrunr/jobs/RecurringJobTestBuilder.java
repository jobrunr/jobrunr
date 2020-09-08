package org.jobrunr.jobs;

import org.jobrunr.jobs.details.JobDetailsAsmGenerator;
import org.jobrunr.jobs.lambdas.IocJobLambda;
import org.jobrunr.jobs.lambdas.JobLambda;
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
                .withName("a recurring job")
                .withJobDetails(defaultJobDetails())
                .withCronExpression(Cron.daily(9, 30))
                .withZoneId(ZoneId.systemDefault());
    }

    public RecurringJobTestBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public RecurringJobTestBuilder withoutId() {
        this.id = null;
        return this;
    }

    public RecurringJobTestBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public RecurringJobTestBuilder withJobDetails(JobLambda jobLambda) {
        this.jobDetails = new JobDetailsAsmGenerator().toJobDetails(jobLambda);
        return this;
    }

    public RecurringJobTestBuilder withJobDetails(IocJobLambda jobLambda) {
        this.jobDetails = new JobDetailsAsmGenerator().toJobDetails(jobLambda);
        return this;
    }

    public RecurringJobTestBuilder withJobDetails(JobDetailsTestBuilder jobDetailsBuilder) {
        withJobDetails(jobDetailsBuilder.build());
        return this;
    }

    public RecurringJobTestBuilder withJobDetails(JobDetails jobDetails) {
        this.jobDetails = jobDetails;
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