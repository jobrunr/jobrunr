package org.jobrunr.scheduling;

import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.details.JobDetailsGenerator;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.scheduling.cron.CronExpression;
import org.jobrunr.scheduling.interval.Interval;

import java.time.Duration;
import java.time.ZoneId;

import static java.time.ZoneId.systemDefault;

public class RecurringJobBuilder {

    private String jobId;
    private String jobName;
    private Integer retries;
    private JobLambda jobLambda;
    private JobRequest jobRequest;
    private Schedule schedule;
    private ZoneId zoneId;

    private RecurringJobBuilder() {
        // why: builder pattern
    }

    public static RecurringJobBuilder aRecurringJob() {
        return new RecurringJobBuilder();
    }

    public RecurringJobBuilder withId(String jobId) {
        this.jobId = jobId;
        return this;
    }

    public RecurringJobBuilder withName(String jobName) {
        this.jobName = jobName;
        return this;
    }

    public RecurringJobBuilder withAmountOfRetries(int amountOfRetries) {
        this.retries = amountOfRetries;
        return this;
    }

    public RecurringJobBuilder withDetails(JobLambda jobLambda) {
        if (this.jobRequest != null) {
            throw new IllegalArgumentException("withJobRequest() is already called, only 1 of [withDetails(), withJobRequest()] should be called.");
        }
        this.jobLambda = jobLambda;
        return this;
    }

    public RecurringJobBuilder withJobRequest(JobRequest jobRequest) {
        if (this.jobLambda != null) {
            throw new IllegalArgumentException("withJobLambda() is already called, only 1 of [withDetails(), withJobRequest()] should be called.");
        }
        this.jobRequest = jobRequest;
        return this;
    }

    public RecurringJobBuilder withCron(String cron) {
        if (this.schedule != null) {
            throw new IllegalArgumentException("A schedule has already been provided.");
        }
        this.schedule = CronExpression.create(cron);
        return this;
    }

    public RecurringJobBuilder withDuration(Duration duration) {
        if (this.schedule != null) {
            throw new IllegalArgumentException("A schedule has already been provided.");
        }
        this.schedule = new Interval(duration);
        return this;
    }

    public RecurringJobBuilder withZoneId(String zoneId) {
        this.zoneId = ZoneId.of(zoneId);
        return this;
    }

    public RecurringJobBuilder withZoneId(ZoneId zoneId) {
        this.zoneId = zoneId;
        return this;
    }

    protected RecurringJob build() {
        if (this.jobLambda != null) {
            throw new IllegalArgumentException("withJobLambda() is already called, only 1 of [withDetails(), withJobRequest()] should be called.");
        }
        JobDetails jobDetails = new JobDetails(jobRequest);
        return this.build(jobDetails);
    }

    protected RecurringJob build(JobDetailsGenerator jobDetailsGenerator) {
        if (jobLambda == null) {
            throw new IllegalArgumentException("JobLambda must be present.");
        }
        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(jobLambda);
        return this.build(jobDetails);
    }

    private RecurringJob build(JobDetails jobDetails) {
        if (schedule == null) {
            throw new IllegalArgumentException("A schedule must be present. Please call withCron() or withDuration().");
        }
        if (zoneId == null) {
            zoneId = systemDefault();
        }
        RecurringJob recurringJob = new RecurringJob(jobId, jobDetails, schedule, zoneId);
        setJobName(recurringJob);
        setAmountOfRetries(recurringJob);
        return recurringJob;
    }

    private void setJobName(RecurringJob recurringJob) {
        if (jobName != null) {
            recurringJob.setJobName(jobName);
        }
    }

    private void setAmountOfRetries(RecurringJob recurringJob) {
        if (retries != null) {
            recurringJob.setAmountOfRetries(retries);
        }
    }
}
