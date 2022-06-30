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
    //TODO RB jobDetails isn't a JobDetails object, consider renaming
    //TODO RB possible names: jobMethod, jobWork, jobContent?
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
        this.retries = amountOfRetries;
        return self();
    }

    public T withDetails(J jobDetails) {
        this.jobDetails = jobDetails;
        return self();
    }

    protected Job build(JobDetails jobDetails) {
        Job job;
        if(jobId != null) {
            job = new Job(jobId, jobDetails, getState());
        } else {
            job = new Job(jobDetails, getState());
        }
        setJobName(job);
        setAmountOfRetries(job);
        return job;
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
