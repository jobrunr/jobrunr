package org.jobrunr.scheduling;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.states.AbstractJobState;
import org.jobrunr.jobs.states.EnqueuedState;
import org.jobrunr.jobs.states.ScheduledState;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public abstract class AbstractJobBuilder<T extends AbstractJobBuilder> {

    private UUID jobId;
    private String jobName;
    private Instant scheduleAt;
    private Integer retries;

    protected AbstractJobBuilder() {
        // why: builder pattern
    }

    protected abstract T self();

    /**
     * Allows to set the id of the job. If a job with that id already exists, JobRunr will not save it again.
     * @param jobId the id of the job
     * @return  the same builder instance which provides a fluent api
     */
    public T withId(UUID jobId) {
        this.jobId = jobId;
        return self();
    }

    /**
     * Allows to set the name of the job for the dashboard.
     * @param jobName the name of the job for the dashboard
     * @return  the same builder instance which provides a fluent api
     */
    public T withName(String jobName) {
        this.jobName = jobName;
        return self();
    }

    /**
     * Allows to specify the duration after which the job should be enqueued.
     * @param duration the duration after which the job should be enqueued
     * @return  the same builder instance which provides a fluent api
     */
    public T scheduleIn(Duration duration) {
        this.scheduleAt = Instant.now().plus(duration);
        return self();
    }

    /**
     * Allows to specify the instant on which the job will be enqueued.
     * @param scheduleAt the instant on which the job will be enqueued
     * @return  the same builder instance which provides a fluent api
     */
    public T scheduleAt(Instant scheduleAt) {
        this.scheduleAt = scheduleAt;
        return self();
    }

    /**
     * Allows to specify the amount of retries for a job when it fails
     * @param amountOfRetries the amount of retries that JobRunr will perform in case the job fails
     * @return  the same builder instance which provides a fluent api
     */
    public T withAmountOfRetries(int amountOfRetries) {
        this.retries = amountOfRetries;
        return self();
    }

    protected Job build(JobDetails jobDetails) {
        Job job = new Job(jobId, jobDetails, getState());
        setJobName(job);
        setAmountOfRetries(job);
        return job;
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
