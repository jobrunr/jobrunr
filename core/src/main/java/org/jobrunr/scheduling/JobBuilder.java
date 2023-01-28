package org.jobrunr.scheduling;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.details.JobDetailsGenerator;
import org.jobrunr.jobs.lambdas.IocJobLambda;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRunrJob;
import org.jobrunr.jobs.states.AbstractJobState;
import org.jobrunr.jobs.states.EnqueuedState;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.utils.JobUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.jobrunr.utils.CollectionUtils.asSet;

/**
 * This class is used to build a {@link Job} using a job lambda or a {@Link JobRequest}.
 * <p>
 * You can use it as follows:
 * <h5>A job lambda example:</h5>
 * <pre>{@code
 *            MyService service = new MyService();
 *            jobScheduler.create(aJob()
 *                                  .withId(UUID.fromString(idRequestParam))
 *                                  .withAmountOfRetries(3)
 *                                  .scheduleIn(Duration.ofSeconds(10))
 *                                  .withDetails(() -> service.sendMail(toRequestParam, subjectRequestParam, bodyRequestParam));
 *       }</pre>
 * <h5>A JobRequest example:</h5>
 * <pre>{@code
 *          jobRequestScheduler.create(aJob()
 *                                .withId(UUID.fromString(idRequestParam))
 *                                .withAmountOfRetries(3)
 *                                .scheduleIn(Duration.ofSeconds(10))
 *                                .withJobRequest(new SendMailRequest(toRequestParam, subjectRequestParam, bodyRequestParam));
 *     }</pre>
 */
public class JobBuilder {

    private JobBuilder() {
        // why: builder pattern
    }

    private UUID jobId;
    private String jobName;
    private Instant scheduleAt;
    private Integer retries;
    private Set<String> labels;
    private JobRunrJob jobLambda;
    private JobRequest jobRequest;

    /**
     * Creates a new {@link JobBuilder} instance to create a Job using a Java 8 lambda.
     *
     * @return a new {@link JobBuilder} instance
     */
    public static JobBuilder aJob() {
        return new JobBuilder();
    }

    /**
     * Allows to set the id of the job. If a job with that id already exists, JobRunr will not save it again.
     * @param jobId the id of the job
     * @return  the same builder instance which provides a fluent api
     */
    public JobBuilder withId(UUID jobId) {
        this.jobId = jobId;
        return this;
    }

    /**
     * Allows to set the name of the job for the dashboard.
     * @param jobName the name of the job for the dashboard
     * @return  the same builder instance which provides a fluent api
     */
    public JobBuilder withName(String jobName) {
        this.jobName = jobName;
        return this;
    }

    /**
     * Allows to specify the duration after which the job should be enqueued.
     * @param duration the duration after which the job should be enqueued
     * @return  the same builder instance which provides a fluent api
     */
    public JobBuilder scheduleIn(Duration duration) {
        this.scheduleAt = Instant.now().plus(duration);
        return this;
    }

    /**
     * Allows to specify the instant on which the job will be enqueued.
     * @param scheduleAt the instant on which the job will be enqueued
     * @return  the same builder instance which provides a fluent api
     */
    public JobBuilder scheduleAt(Instant scheduleAt) {
        this.scheduleAt = scheduleAt;
        return this;
    }

    /**
     * Allows to specify the amount of retries for a job when it fails
     *
     * @param amountOfRetries the amount of retries that JobRunr will perform in case the job fails
     * @return the same builder instance which provides a fluent api
     */
    public JobBuilder withAmountOfRetries(int amountOfRetries) {
        this.retries = amountOfRetries;
        return this;
    }

    /**
     * Allows to provide a set of labels to be shown in the dashboard.
     * A maximum of 3 labels can be provided per job. Each label has a max length of 45 characters.
     *
     * @param labels an array of labels to be added to the recurring job
     * @return the same builder instance which provides a fluent api
     */
    public JobBuilder withLabels(String... labels) {
        return withLabels(asSet(labels));
    }

    /**
     * Allows to provide a set of labels to be shown in the dashboard.
     * A maximum of 3 labels can be provided per job. Each label has a max length of 45 characters.
     *
     * @param labels an array of labels to be added to the recurring job
     * @return the same builder instance which provides a fluent api
     */
    public JobBuilder withLabels(Set<String> labels) {
        this.labels = labels;
        return this;
    }

    /**
     * Allows to provide the job details by means of Java 8 lambda.
     *
     * @param jobLambda the lambda which defines the job
     * @return the same builder instance that can be given to the {@link JobScheduler#create(JobBuilder)} method
     */
    public JobBuilder withDetails(JobLambda jobLambda) {
        if (this.jobRequest != null) {
            throw new IllegalArgumentException("withJobRequest() is already called, only 1 of [withDetails(), withJobRequest()] should be called.");
        }
        this.jobLambda = jobLambda;
        return this;
    }

    /**
     * Allows to provide the job details by means of Java 8 lambda. The IoC container will be used to resolve an actual instance of the requested service.
     *
     * @param jobLambda the lambda which defines the job
     * @return the same builder instance that can be given to the {@link JobScheduler#create(JobBuilder)} method
     */
    public <S> JobBuilder withDetails(IocJobLambda<S> jobLambda) {
        if (this.jobRequest != null) {
            throw new IllegalArgumentException("withJobRequest() is already called, only 1 of [withDetails(), withJobRequest()] should be called.");
        }
        this.jobLambda = jobLambda;
        return this;
    }

    /**
     * Allows to provide the job details by means of {@link JobRequest}.
     *
     * @param jobRequest the jobRequest which defines the job.
     * @return the same builder instance that can be given to the {@link JobRequestScheduler#create(JobBuilder)} method
     */
    public JobBuilder withJobRequest(JobRequest jobRequest) {
        if (this.jobLambda != null) {
            throw new IllegalArgumentException("withJobLambda() is already called, only 1 of [withDetails(), withJobRequest()] should be called.");
        }
        this.jobRequest = jobRequest;
        return this;
    }

    /**
     * Not publicly visible as it will be used by the {@link JobScheduler} only.
     *
     * @return the actual {@link Job} to create
     */
    protected Job build(JobDetailsGenerator jobDetailsGenerator) {
        if(jobLambda == null) {
            throw new IllegalArgumentException("A jobLambda must be present.");
        }
        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(jobLambda);
        return build(jobDetails);
    }

    /**
     * Not publicly visible as it will be used by the {@link JobRequestScheduler} only.
     *
     * @return the actual {@link Job} to create
     */
    protected Job build() {
        if(jobRequest == null) {
            throw new IllegalArgumentException("JobRequest must be present.");
        }
        JobDetails jobDetails = new JobDetails(jobRequest);
        return build(jobDetails);
    }

    private Job build(JobDetails jobDetails) {
        if(JobUtils.getJobAnnotation(jobDetails).isPresent()) {
            throw new IllegalStateException("You are combining the JobBuilder with the Job annotation which is not allowed. You can only use one of them.");
        }

        Job job = new Job(jobId, jobDetails, getState());
        setJobName(job);
        setAmountOfRetries(job);
        setLabels(job);
        return job;
    }

    private void setJobName(Job job) {
        if(jobName != null) {
            job.setJobName(jobName);
        }
    }

    private void setAmountOfRetries(Job job) {
        if (retries != null) {
            job.setAmountOfRetries(retries);
        }
    }

    private void setLabels(Job job) {
        if (labels != null) {
            job.setLabels(labels);
        }
    }

    private AbstractJobState getState() {
        if (this.scheduleAt != null) {
            return new ScheduledState(this.scheduleAt);
        } else {
            return new EnqueuedState();
        }
    }
}
