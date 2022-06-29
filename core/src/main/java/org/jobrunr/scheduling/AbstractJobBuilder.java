package org.jobrunr.scheduling;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.states.AbstractJobState;
import org.jobrunr.jobs.states.EnqueuedState;
import org.jobrunr.jobs.states.ScheduledState;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public abstract class AbstractJobBuilder<T extends AbstractJobBuilder, J> {

    private UUID jobId;
    private String jobName;
    private Instant scheduleAt;
    private Integer retries;
    private J jobDetails;

    protected AbstractJobBuilder() {
        // why: builder pattern
    }

    protected abstract T self();

    public T withId(UUID jobId) {
        this.jobId = jobId;
        return self();
    }

    public T withName(String jobName) {
        this.jobName = jobName;
        return self();
    }

    public T scheduleIn(Duration duration) {
        this.scheduleAt = Instant.now().plus(duration);
        return self();
    }

    public T scheduleAt(Instant scheduleAt) {
        this.scheduleAt = scheduleAt;
        return self();
    }

    public T withAmountOfRetries(int amountOfRetries) {
        this.retries = retries;
        return self();
    }

    public T withDetails(J jobLambda) {
        this.jobDetails = jobLambda;
        return self();
    }

    protected Job createJob(JobDetails jobDetails) {
        if(jobId != null) {
            return new Job(jobId, jobDetails, getState());
        } else {
            return new Job(jobDetails, getState());
        }
    }

    protected UUID getJobId() {
        return jobId;
    }

    protected J getJobDetails() {
        return jobDetails;
    }

    protected void setJobName(Job job) {
        if(jobName != null) {
            job.setJobName(jobName);
        }
    }

    protected void setAmountOfRetries(Job job) {
        if(retries != null) {
            job.setAmountOfRetries(retries);
        }
    }

    protected AbstractJobState getState() {
        if(this.scheduleAt != null) {
            return new ScheduledState(this.scheduleAt);
        } else {
            return new EnqueuedState();
        }
    }
}
