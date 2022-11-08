package org.jobrunr.scheduling;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.details.JobDetailsGenerator;
import org.jobrunr.jobs.lambdas.JobLambda;

/**
 * This class is used to build a {@link Job} using a job lambda that can be enqueued or scheduled and provides an alternative to the job annotation.
 *
 * You can use it as follows:
 * <h5>An example:</h5>
 * <pre>{@code
 *            MyService service = new MyService();
 *            jobScheduler.create(aJob()
 *                                  .withId(UUID.fromString(idRequestParam))
 *                                  .withAmountOfRetries(3)
 *                                  .scheduleIn(Duration.ofSeconds(10))
 *                                  .withDetails(() -> service.sendMail(toRequestParam, subjectRequestParam, bodyRequestParam));
 *       }</pre>
 *
 */
public class JobBuilder extends AbstractJobBuilder<JobBuilder> {

    private JobBuilder() {
        // why: builder pattern
    }

    private JobLambda jobLambda;

    /**
     * Creates a new {@link JobBuilder} instance to create a Job using a Java 8 lambda.
     *
     * @return a new {@link JobBuilder} instance
     */
    public static JobBuilder aJob() {
        return new JobBuilder();
    }

    /**
     * Allows to provide the job details by means of Java 8 lambda.
     * @param jobLambda the lambda which defines the job
     * @return the same builder instance that can be given to the {@link JobScheduler#create(JobBuilder)} method
     */
    public JobBuilder withDetails(JobLambda jobLambda) {
        this.jobLambda = jobLambda;
        return this;
    }

    /**
     * Not publicly visible as it will be used by the {@link JobScheduler} only.
     * @return the actual {@link Job} to create
     */
    Job build(JobDetailsGenerator jobDetailsGenerator) {
        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(jobLambda);
        return super.build(jobDetails);
    }

    @Override
    protected JobBuilder self() {
        return this;
    }
}
