package org.jobrunr.scheduling;

import org.jobrunr.jobs.Job;
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

/**
 * This class is used to build a {@link RecurringJob} using a job lambda or a {@Link JobRequest}.
 * <p>
 * You can use it as follows:
 * <h5>A job lambda example:</h5>
 * <pre>{@code
 *            MyService service = new MyService();
 *            jobScheduler.scheduleRecurrently(aRecurringJob()
 *                                  .withCron("* * 0 * * *")
 *                                  .withDetails(() -> service.sendMail(toRequestParam, subjectRequestParam, bodyRequestParam));
 *       }</pre>
 * <h5>A JobRequest example:</h5>
 * <pre>{@code
 *          MyService service = new MyService();
 *          jobRequestScheduler.scheduleRecurrently(aRecurringJob()
 *                                .withCron("* * 0 * * *")
 *                                .withJobRequest(new SendMailRequest(toRequestParam, subjectRequestParam, bodyRequestParam));
 *     }</pre>
 */
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

    /**
     * Creates a new {@link RecurringJobBuilder} instance to create a {@link RecurringJob} using a Java 8 lambda.
     *
     * @return a new {@link RecurringJobBuilder} instance
     */
    public static RecurringJobBuilder aRecurringJob() {
        return new RecurringJobBuilder();
    }

    /**
     * Allows to set the id of the recurringJob. If a recurringJob with that id already exists, JobRunr will not save it again.
     *
     * @param jobId the recurringJob of the recurringJob
     * @return the same builder instance which provides a fluent api
     */
    public RecurringJobBuilder withId(String jobId) {
        this.jobId = jobId;
        return this;
    }

    /**
     * Allows to set the name of the recurringJob for the dashboard.
     *
     * @param jobName the name of the recurringJob for the dashboard
     * @return  the same builder instance which provides a fluent api
     */
    public RecurringJobBuilder withName(String jobName) {
        this.jobName = jobName;
        return this;
    }

    /**
     * Allows to specify number of times that the recurringJob should be retried.
     *
     * @param amountOfRetries the amount of times the recurringJob should be retried.
     * @return  the same builder instance which provides a fluent api
     */
    public RecurringJobBuilder withAmountOfRetries(int amountOfRetries) {
        this.retries = amountOfRetries;
        return this;
    }

    /**
     * Allows to provide the job details by means of Java 8 lambda.
     *
     * @param jobLambda the lambda which defines the job
     * @return the same builder instance that can be given to the {@link JobScheduler#scheduleRecurrently(RecurringJobBuilder)} method
     */
    public RecurringJobBuilder withDetails(JobLambda jobLambda) {
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
     * @return the same builder instance that can be given to the {@link JobRequestScheduler#scheduleRecurrently(RecurringJobBuilder)} method
     */
    public RecurringJobBuilder withJobRequest(JobRequest jobRequest) {
        if (this.jobLambda != null) {
            throw new IllegalArgumentException("withJobLambda() is already called, only 1 of [withDetails(), withJobRequest()] should be called.");
        }
        this.jobRequest = jobRequest;
        return this;
    }

    /**
     * Allows to specify the cron that will be used to create the recurringJobs.
     *
     * @param cron the cron that will be used to create the recurringJobs.
     * @return  the same builder instance which provides a fluent api
     */
    public RecurringJobBuilder withCron(String cron) {
        if (this.schedule != null) {
            throw new IllegalArgumentException("A schedule has already been provided.");
        }
        this.schedule = CronExpression.create(cron);
        return this;
    }

    /**
     * Allows to specify the duration that will be used to create the recurringJobs.
     *
     * @param duration the duration that will be used to create the recurringJobs.
     * @return  the same builder instance which provides a fluent api
     */
    public RecurringJobBuilder withDuration(Duration duration) {
        if (this.schedule != null) {
            throw new IllegalArgumentException("A schedule has already been provided.");
        }
        this.schedule = new Interval(duration);
        return this;
    }

    /**
     * Allows to specify the zoneId that will be used to create the recurringJobs.
     * If no zoneId is set, the {@link ZoneId#systemDefault()} is used.
     *
     * @param zoneId the zoneId that will be used to create the recurringJobs.
     * @return  the same builder instance which provides a fluent api
     */
    public RecurringJobBuilder withZoneId(ZoneId zoneId) {
        this.zoneId = zoneId;
        return this;
    }

    /**
     * Not publicly visible as it will be used by the {@link JobScheduler} only.
     *
     * @return the actual {@link Job} to create
     */
    protected RecurringJob build(JobDetailsGenerator jobDetailsGenerator) {
        if (jobLambda == null) {
            throw new IllegalArgumentException("JobLambda must be present.");
        }
        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(jobLambda);
        return this.build(jobDetails);
    }

    /**
     * Not publicly visible as it will be used by the {@link JobRequestScheduler} only.
     *
     * @return the actual {@link RecurringJob} to create
     */
    protected RecurringJob build() {
        if (jobRequest == null) {
            throw new IllegalArgumentException("JobRequest must be present.");
        }
        JobDetails jobDetails = new JobDetails(jobRequest);
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
