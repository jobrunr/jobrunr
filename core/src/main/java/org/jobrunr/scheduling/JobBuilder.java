package org.jobrunr.scheduling;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.details.JobDetailsGenerator;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.jobs.states.AbstractJobState;
import org.jobrunr.jobs.states.EnqueuedState;
import org.jobrunr.jobs.states.ScheduledState;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class JobBuilder {

    private UUID jobId;
    private String jobName;
    private Instant scheduleAt;
    private Integer retries;
    private JobLambda jobLambda;

    private JobBuilder() {
        // why: builder pattern
    }

    public static JobBuilder aJob() {
        return new JobBuilder();
    }

    public JobBuilder withId(UUID jobId) {
        this.jobId = jobId;
        return this;
    }

    public JobBuilder withName(String jobName) {
        this.jobName = jobName;
        return this;
    }

    public JobBuilder scheduleIn(Duration duration) {
        this.scheduleAt = Instant.now().plus(duration);
        return this;
    }

    public JobBuilder scheduleAt(Instant scheduleAt) {
        this.scheduleAt = scheduleAt;
        return this;
    }

    public JobBuilder withAmountOfRetries(int amountOfRetries) {
        this.retries = retries;
        return this;
    }

    public JobBuilder withDetails(JobLambda jobLambda) {
        this.jobLambda = jobLambda;
        return this;
    }

    Job toJob(JobDetailsGenerator jobDetailsGenerator) {
        Job job = createJob(jobDetailsGenerator);
        setJobName(job);
        setAmountOfRetries(job);
        return job;
    }

    private Job createJob(JobDetailsGenerator jobDetailsGenerator) {
        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(jobLambda);
        if(jobId != null) {
            return new Job(jobId, jobDetails, getState());
        } else {
            return new Job(jobDetails, getState());
        }
    }

    private void setJobName(Job job) {
        if(jobName != null) {
            job.setJobName(jobName);
        }
    }

    private void setAmountOfRetries(Job job) {
        if(retries != null) {
            job.setAmountOfRetries(retries);
        }
    }

    private AbstractJobState getState() {
        if(this.scheduleAt != null) {
            return new ScheduledState(this.scheduleAt);
        } else {
            return new EnqueuedState();
        }
    }
}
