package org.jobrunr.scheduling;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.lambdas.JobRequest;

/**
 * This class is used to build a {@link Job} using a {@link JobRequest} that can be enqueued or scheduled and provides an alternative to the job annotation.
 *
 * You can use it as follows:
 * <h5>An example:</h5>
 * <pre>{@code
 *            MyService service = new MyService();
 *            jobScheduler.create(aJob()
 *                                  .withId(UUID.fromString(idRequestParam))
 *                                  .withAmountOfRetries(3)
 *                                  .scheduleIn(Duration.ofSeconds(10))
 *                                  .withJobRequest(new SendMailRequest(toRequestParam, subjectRequestParam, bodyRequestParam));
 *       }</pre>
 *
 */
public class JobRequestBuilder extends AbstractJobBuilder<JobRequestBuilder> {

    private JobRequestBuilder() {
        // why: builder pattern
    }

    private JobRequest jobRequest;

    /**
     * Creates a new {@link JobRequestBuilder} instance to create a Job using a {@link JobRequest}.
     *
     * @return a new {@link JobRequestBuilder} instance
     */
    public static JobRequestBuilder aJob() {
        return new JobRequestBuilder();
    }

    /**
     * Allows to provide the job details by means of {@link JobRequest}.
     * @param jobRequest the jobRequest which defines the job.
     * @return the same builder instance that can be given to the {@link JobRequestScheduler#create(JobRequestBuilder)} method
     */
    public JobRequestBuilder withJobRequest(JobRequest jobRequest) {
        this.jobRequest = jobRequest;
        return this;
    }

    /**
     * Not publicly visible as it will be used by the {@link JobRequestScheduler} only.
     * @return the actual {@link Job} to create
     */
    Job build() {
        JobDetails jobDetails = new JobDetails(jobRequest);
        return super.build(jobDetails);
    }

    @Override
    protected JobRequestBuilder self() {
        return this;
    }
}
