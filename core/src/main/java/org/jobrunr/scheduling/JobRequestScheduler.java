package org.jobrunr.scheduling;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.filters.JobFilter;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.scheduling.cron.CarbonAwareCronExpression;
import org.jobrunr.scheduling.cron.CronExpression;
import org.jobrunr.scheduling.interval.Interval;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.carbonaware.CarbonAwarePeriod;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static java.time.ZoneId.systemDefault;
import static java.util.Collections.emptyList;
import static org.jobrunr.storage.StorageProvider.BATCH_SIZE;
import static org.jobrunr.utils.streams.StreamUtils.batchCollector;

/**
 * Provides methods for creating fire-and-forget, delayed and recurring jobs as well as to delete existing background jobs.
 * <p>
 * This {@code JobRequestScheduler} allows to schedule jobs by means of an implementation of a {@code JobRequest}.
 *
 * @author Ronald Dehuysser
 */
public class JobRequestScheduler extends AbstractJobScheduler {

    /**
     * Creates a new {@link JobRequestScheduler} using the provided {@link StorageProvider}
     *
     * @param storageProvider the storageProvider to use
     */
    public JobRequestScheduler(StorageProvider storageProvider) {
        super(storageProvider, emptyList());
    }

    /**
     * Creates a new JobRequestScheduler using the provided {@link StorageProvider} and the list of JobFilters that will be used for every background job
     *
     * @param storageProvider the storageProvider to use
     * @param jobFilters      list of jobFilters that will be used for every job
     */
    public JobRequestScheduler(StorageProvider storageProvider, List<JobFilter> jobFilters) {
        super(storageProvider, jobFilters);
        BackgroundJobRequest.setJobRequestScheduler(this);
    }

    /**
     * Creates a new {@link org.jobrunr.jobs.Job} using a {@link JobBuilder} that can be enqueued or scheduled and provides an alternative to the job annotation.
     *
     * @param jobBuilder the {@link JobBuilder} with all the details of the job
     * @return the id of the job
     */
    @Override
    public JobId create(JobBuilder jobBuilder) {
        Job job = jobBuilder.build();
        return saveJob(job);
    }

    /**
     * Creates a new {@link org.jobrunr.jobs.Job} for each {@link JobBuilder} and provides an alternative to the job annotation.
     *
     * @param jobBuilderStream the jobBuilders for which to create jobs.
     */
    @Override
    public void create(Stream<JobBuilder> jobBuilderStream) {
        jobBuilderStream
                .map(JobBuilder::build)
                .collect(batchCollector(BATCH_SIZE, this::saveJobs));
    }

    /**
     * Creates a new fire-and-forget job based on a given {@link JobRequest}. JobRunr will try to find the JobRequestHandler in
     * the IoC container or else it will try to create the handler by calling the default no-arg constructor.
     * <h5>An example:</h5>
     * <pre>{@code
     *            jobScheduler.enqueue(new MyJobRequest());
     *       }</pre>
     *
     * @param jobRequest the jobRequest which defines the fire-and-forget job.
     * @return the id of the job
     */
    public JobId enqueue(JobRequest jobRequest) {
        return enqueue(null, jobRequest);
    }

    /**
     * Creates a new fire-and-forget job based on a given {@link JobRequest}. JobRunr will try to find the JobRequestHandler in
     * the IoC container or else it will try to create the handler by calling the default no-arg constructor.
     * <h5>An example:</h5>
     * <pre>{@code
     *            jobScheduler.enqueue(id, new MyJobRequest());
     *       }</pre>
     *
     * @param id         the uuid with which to save the job
     * @param jobRequest the jobRequest which defines the fire-and-forget job.
     * @return the id of the job
     */
    public JobId enqueue(UUID id, JobRequest jobRequest) {
        JobDetails jobDetails = new JobDetails(jobRequest);
        return enqueue(id, jobDetails);
    }

    /**
     * Creates new fire-and-forget jobs for each item in the input stream. JobRunr will try to find the JobRequestHandler in
     * the IoC container or else it will try to create the handler by calling the default no-arg constructor.
     * <h5>An example:</h5>
     * <pre>{@code
     *      Stream<MyJobRequest> workStream = getWorkStream();
     *      jobScheduler.enqueue(workStream);
     * }</pre>
     *
     * @param input the stream of jobRequests for which to create fire-and-forget jobs
     */
    public void enqueue(Stream<? extends JobRequest> input) {
        input
                .map(JobDetails::new)
                .map(Job::new)
                .collect(batchCollector(BATCH_SIZE, this::saveJobs));
    }

    /**
     * Creates a new fire-and-forget job based on the given {@link JobRequest} and schedules it to be enqueued at the given moment of time. JobRunr will try to find the JobRequestHandler in
     * the IoC container or else it will try to create the handler by calling the default no-arg constructor.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.schedule(ZonedDateTime.now().plusHours(5), new MyJobRequest());
     * }</pre>
     *
     * @param zonedDateTime the moment in time at which the job will be enqueued.
     * @param jobRequest    the jobRequest which defines the fire-and-forget job
     * @return the id of the Job
     */
    public JobId schedule(ZonedDateTime zonedDateTime, JobRequest jobRequest) {
        return schedule(null, zonedDateTime.toInstant(), jobRequest);
    }

    /**
     * Creates a new fire-and-forget job based on the given {@link JobRequest} and schedules it to be enqueued at the given moment of time. JobRunr will try to find the JobRequestHandler in
     * the IoC container or else it will try to create the handler by calling the default no-arg constructor.
     * If a job with that id already exists, JobRunr will not save it again.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.schedule(id, ZonedDateTime.now().plusHours(5), new MyJobRequest());
     * }</pre>
     *
     * @param id            the uuid with which to save the job
     * @param zonedDateTime the moment in time at which the job will be enqueued.
     * @param jobRequest    the jobRequest which defines the fire-and-forget job
     * @return the id of the Job
     */
    public JobId schedule(UUID id, ZonedDateTime zonedDateTime, JobRequest jobRequest) {
        return schedule(id, zonedDateTime.toInstant(), jobRequest);
    }

    /**
     * Creates a new fire-and-forget job based on the given {@link JobRequest} and schedules it to be enqueued at the given moment of time. JobRunr will try to find the JobRequestHandler in
     * the IoC container or else it will try to create the handler by calling the default no-arg constructor.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.schedule(OffsetDateTime.now().plusHours(5), new MyJobRequest());
     * }</pre>
     *
     * @param offsetDateTime the moment in time at which the job will be enqueued.
     * @param jobRequest     the jobRequest which defines the fire-and-forget job
     * @return the id of the Job
     */
    public JobId schedule(OffsetDateTime offsetDateTime, JobRequest jobRequest) {
        return schedule(null, offsetDateTime.toInstant(), jobRequest);
    }

    /**
     * Creates a new fire-and-forget job based on the given {@link JobRequest} and schedules it to be enqueued at the given moment of time. JobRunr will try to find the JobRequestHandler in
     * the IoC container or else it will try to create the handler by calling the default no-arg constructor.
     * If a job with that id already exists, JobRunr will not save it again.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.schedule(id, OffsetDateTime.now().plusHours(5), new MyJobRequest());
     * }</pre>
     *
     * @param id             the uuid with which to save the job
     * @param offsetDateTime the moment in time at which the job will be enqueued.
     * @param jobRequest     the jobRequest which defines the fire-and-forget job
     * @return the id of the Job
     */
    public JobId schedule(UUID id, OffsetDateTime offsetDateTime, JobRequest jobRequest) {
        return schedule(id, offsetDateTime.toInstant(), jobRequest);
    }

    /**
     * Creates a new fire-and-forget job based on the given {@link JobRequest} and schedules it to be enqueued at the given moment of time. JobRunr will try to find the JobRequestHandler in
     * the IoC container or else it will try to create the handler by calling the default no-arg constructor.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.schedule(LocalDateTime.now().plusHours(5), new MyJobRequest());
     * }</pre>
     *
     * @param localDateTime the moment in time at which the job will be enqueued. It will use the systemDefault ZoneId to transform it to an UTC Instant
     * @param jobRequest    the jobRequest which defines the fire-and-forget job
     * @return the id of the Job
     */
    public JobId schedule(LocalDateTime localDateTime, JobRequest jobRequest) {
        return schedule(localDateTime.atZone(systemDefault()).toInstant(), jobRequest);
    }

    /**
     * Creates a new fire-and-forget job based on the given {@link JobRequest} and schedules it to be enqueued at the given moment of time. JobRunr will try to find the JobRequestHandler in
     * the IoC container or else it will try to create the handler by calling the default no-arg constructor.
     * If a job with that id already exists, JobRunr will not save it again.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.schedule(id, LocalDateTime.now().plusHours(5), new MyJobRequest());
     * }</pre>
     *
     * @param id            the uuid with which to save the job
     * @param localDateTime the moment in time at which the job will be enqueued. It will use the systemDefault ZoneId to transform it to an UTC Instant
     * @param jobRequest    the jobRequest which defines the fire-and-forget job
     * @return the id of the Job
     */
    public JobId schedule(UUID id, LocalDateTime localDateTime, JobRequest jobRequest) {
        return schedule(id, localDateTime.atZone(systemDefault()).toInstant(), jobRequest);
    }

    /**
     * Creates a new fire-and-forget job based on the given {@link JobRequest} and schedules it to be enqueued at the given moment of time. JobRunr will try to find the JobRequestHandler in
     * the IoC container or else it will try to create the handler by calling the default no-arg constructor.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.schedule(Instant.now().plusHours(5), new MyJobRequest());
     * }</pre>
     *
     * @param instant    the moment in time at which the job will be enqueued.
     * @param jobRequest the jobRequest which defines the fire-and-forget job
     * @return the id of the Job
     */
    public JobId schedule(Instant instant, JobRequest jobRequest) {
        return schedule(null, instant, jobRequest);
    }

    /**
     * Creates a new fire-and-forget job based on the given {@link JobRequest} and schedules it to be enqueued at the given moment of time. JobRunr will try to find the JobRequestHandler in
     * the IoC container or else it will try to create the handler by calling the default no-arg constructor.
     * If a job with that id already exists, JobRunr will not save it again.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.schedule(id, Instant.now().plusHours(5), new MyJobRequest());
     * }</pre>
     *
     * @param id         the uuid with which to save the job
     * @param instant    the moment in time at which the job will be enqueued.
     * @param jobRequest the jobRequest which defines the fire-and-forget job
     * @return the id of the Job
     */
    public JobId schedule(UUID id, Instant instant, JobRequest jobRequest) {
        JobDetails jobDetails = new JobDetails(jobRequest);
        return schedule(id, instant, jobDetails);
    }

    /**
     * Creates a new fire-and-forget job based on the given {@link JobRequest} that will be scheduled inside the given {@link CarbonAwarePeriod}, in the least carbon intense moment.
     * JobRunr will try to find the JobRequestHandler in the IoC container or else it will try to create the handler by calling the default no-arg constructor.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.scheduleCarbonAware(CarbonAwarePeriod.between(Instant.now(), Instant.now().plusHours(6)), new MyJobRequest());
     * }</pre>
     *
     * @param carbonAwarePeriod the period in which the job will be scheduled
     * @param jobRequest        the jobRequest which defines the fire-and-forget job
     **/
    public JobId scheduleCarbonAware(CarbonAwarePeriod carbonAwarePeriod, JobRequest jobRequest) {
        return scheduleCarbonAware(null, carbonAwarePeriod, jobRequest);
    }

    /**
     * Creates a new fire-and-forget job based on the given {@link JobRequest} that will be scheduled inside the given {@link CarbonAwarePeriod}, in the least carbon intense moment.
     * If a job with that id already exists, JobRunr will not save it again.
     * JobRunr will try to find the JobRequestHandler in the IoC container or else it will try to create the handler by calling the default no-arg constructor.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.scheduleCarbonAware(id, CarbonAwarePeriod.between(Instant.now(), Instant.now().plusHours(6)), new MyJobRequest());
     * }</pre>
     *
     * @param id                the uuid with which to save the job
     * @param carbonAwarePeriod the period in which the job will be scheduled
     * @param jobRequest        the {@link JobRequest} which defines the fire-and-forget job
     * @return the id of the Job
     */
    public JobId scheduleCarbonAware(UUID id, CarbonAwarePeriod carbonAwarePeriod, JobRequest jobRequest) {
        JobDetails jobDetails = new JobDetails(jobRequest);
        return scheduleCarbonAware(id, carbonAwarePeriod, jobDetails);
    }

    /**
     * Creates a new or alters the existing {@link RecurringJob} based on the {@link RecurringJobBuilder} (using id, cron expression and {@link JobRequest}). JobRunr will try to find the JobRequestHandler in
     * the IoC container or else it will try to create the handler by calling the default no-arg constructor.
     * If no zoneId is set on the builder the jobs will be scheduled using the systemDefault timezone.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobRequestScheduler.createRecurrently(aRecurringJob()
     *                                          .withCron("* * 0 * * *")
     *                                          .withJobRequest(new SendMailRequest(toRequestParam, subjectRequestParam, bodyRequestParam));
     * }</pre>
     *
     * @param recurringJobBuilder the builder describing your recurring job.
     * @return the id of this {@link RecurringJob} which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    @Override
    public String createRecurrently(RecurringJobBuilder recurringJobBuilder) {
        RecurringJob recurringJob = recurringJobBuilder.build();
        return this.scheduleRecurrently(recurringJob);
    }

    /**
     * Creates a new {@link RecurringJob} based on the given cron expression and the given {@link JobRequest}. JobRunr will try to find the JobRequestHandler in
     * the IoC container or else it will try to create the handler by calling the default no-arg constructor. The jobs will be scheduled using the systemDefault timezone.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.scheduleRecurrently(Cron.daily(), new MyJobRequest());
     * }</pre>
     *
     * @param cron       The cron expression defining when to run this recurring job
     * @param jobRequest the jobRequest which defines the recurring job
     * @return the id of this {@link RecurringJob} which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    public String scheduleRecurrently(String cron, JobRequest jobRequest) {
        return scheduleRecurrently(null, cron, jobRequest);
    }

    /**
     * Creates a new or alters the existing {@link RecurringJob} based on the given id, cron expression and {@link JobRequest}. JobRunr will try to find the JobRequestHandler in
     * the IoC container or else it will try to create the handler by calling the default no-arg constructor. The jobs will be scheduled using the systemDefault timezone
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.scheduleRecurrently("my-recurring-job", Cron.daily(), new MyJobRequest());
     * }</pre>
     *
     * @param id         the id of this {@link RecurringJob} which can be used to alter or delete it
     * @param cron       the cron expression defining when to run this recurring job
     * @param jobRequest the jobRequest which defines the recurring job
     * @return the id of this {@link RecurringJob} which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    public String scheduleRecurrently(String id, String cron, JobRequest jobRequest) {
        return scheduleRecurrently(id, cron, systemDefault(), jobRequest);
    }

    /**
     * Creates a new or alters the existing {@link RecurringJob} based on the given id, cron expression, {@code ZoneId} and {@link JobRequest}. JobRunr will try to find the JobRequestHandler in
     * the IoC container or else it will try to create the handler by calling the default no-arg constructor.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.scheduleRecurrently("my-recurring-job", Cron.daily(), ZoneId.of("Europe/Brussels"), new MyJobRequest());
     * }</pre>
     *
     * @param id         the id of this {@link RecurringJob} which can be used to alter or delete it
     * @param cron       the cron expression defining when to run this recurring job
     * @param zoneId     the zoneId (timezone) of when to run this recurring job
     * @param jobRequest the jobRequest which defines the recurring job
     * @return the id of this {@link RecurringJob} which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    public String scheduleRecurrently(String id, String cron, ZoneId zoneId, JobRequest jobRequest) {
        JobDetails jobDetails = new JobDetails(jobRequest);
        return scheduleRecurrently(id, jobDetails, CronExpression.create(cron), zoneId);
    }

    /**
     * Creates a new {@link RecurringJob} based on the given duration and the given {@link JobRequest}. JobRunr will try to find the JobRequestHandler in
     * the IoC container or else it will try to create the handler by calling the default no-arg constructor. The first run of this {@link RecurringJob} will happen
     * after the given duration unless your duration is smaller or equal than your backgroundJobServer pollInterval.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.scheduleRecurrently(Duration.parse("P5D"), new MyJobRequest());
     * }</pre>
     *
     * @param duration   the duration defining the time between each instance of this recurring job.
     * @param jobRequest the jobRequest which defines the recurring job
     * @return the id of this {@link RecurringJob} which can be used to alter or delete it
     */
    public String scheduleRecurrently(Duration duration, JobRequest jobRequest) {
        return scheduleRecurrently(null, duration, jobRequest);
    }

    /**
     * Creates a new or alters the existing {@link RecurringJob} based on the given id, duration and jobRequest. JobRunr will try to find the JobRequestHandler in
     * the IoC container or else it will try to create the handler by calling the default no-arg constructor. The first run of this {@link RecurringJob} will happen
     * after the given duration unless your duration is smaller or equal than your backgroundJobServer pollInterval.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.scheduleRecurrently("my-recurring-job", Duration.parse("P5D"), new MyJobRequest());
     * }</pre>
     *
     * @param id         the id of this {@link RecurringJob} which can be used to alter or delete it
     * @param duration   the duration defining the time between each instance of this recurring job
     * @param jobRequest the jobRequest which defines the recurring job
     * @return the id of this {@link RecurringJob} which can be used to alter or delete it
     */
    public String scheduleRecurrently(String id, Duration duration, JobRequest jobRequest) {
        JobDetails jobDetails = new JobDetails(jobRequest);
        return scheduleRecurrently(id, jobDetails, new Interval(duration), systemDefault());
    }

    /**
     * Creates a new {@link RecurringJob} based on the given {@link JobRequest} and the given {@link CarbonAwareCronExpression}.
     * JobRunr will try to find the JobRequestHandler in the IoC container or else it will try to create the handler by calling the default no-arg constructor.
     * The jobs will be scheduled using the systemDefault timezone at the time when carbon intensity of electricity is the lowest.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.scheduleRecurrently(CarbonAwareCron.dailyBetween(13, 17), new MyJobRequest());
     * }</pre>
     *
     * @param carbonAwareCron cron expression + allowed duration before + allowed duration after. Allows job to be scheduled at a time when carbon emissions are the lowest, within the given range.
     * @param jobRequest      the {@link JobRequest} which defines the recurring job
     * @return the id of this {@link RecurringJob} which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.CarbonAwareCron
     */
    public String scheduleRecurrently(CarbonAwareCronExpression carbonAwareCron, JobRequest jobRequest) {
        return scheduleRecurrently(null, carbonAwareCron, jobRequest);
    }

    /**
     * Creates a new or alters the existing {@link RecurringJob} based on the given id, {@link CarbonAwareCronExpression} and {@link JobRequest}.
     * JobRunr will try to find the JobRequestHandler in the IoC container or else it will try to create the handler by calling the default no-arg constructor.
     * The jobs will be scheduled using the systemDefault timezone at the time when carbon intensity of electricity is the lowest.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.scheduleRecurrently("my-recurring-job", CarbonAwareCron.dailyBetween(13, 17), new MyJobRequest());
     * }</pre>
     *
     * @param id              the id of this {@link RecurringJob} which can be used to alter or delete it
     * @param carbonAwareCron cron expression + allowed duration before + allowed duration after. Allows job to be scheduled at a time when carbon emissions are the lowest, within the given range.
     * @param jobRequest      the {@link JobRequest} which defines the recurring job
     * @return the id of this {@link RecurringJob} which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.CarbonAwareCron
     */
    public String scheduleRecurrently(String id, CarbonAwareCronExpression carbonAwareCron, JobRequest jobRequest) {
        JobDetails jobDetails = new JobDetails(jobRequest);
        return scheduleRecurrently(id, jobDetails, carbonAwareCron, systemDefault());
    }

    /**
     * Creates a new or alters the existing {@link RecurringJob} based on the given id, {@link CarbonAwareCronExpression}, {@code ZoneId} and {@link JobRequest}.
     * JobRunr will try to find the JobRequestHandler in the IoC container or else it will try to create the handler by calling the default no-arg constructor.
     * The jobs will be scheduled at the time when carbon intensity of electricity is the lowest.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.scheduleRecurrently("my-recurring-job", CarbonAwareCron.dailyBetween(13, 17), ZoneId.of("Europe/Brussels"), new MyJobRequest());
     * }</pre>
     *
     * @param id              the id of this {@link RecurringJob} which can be used to alter or delete it
     * @param carbonAwareCron cron expression + allowed duration before + allowed duration after. Allows job to be scheduled at a time when carbon emissions are the lowest, within the given range.
     * @param zoneId          the zoneId (timezone) of when to run this recurring job
     * @param jobRequest      the {@link JobRequest} which defines the recurring job
     * @return the id of this {@link RecurringJob} which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.CarbonAwareCron
     */
    public String scheduleRecurrently(String id, CarbonAwareCronExpression carbonAwareCron, ZoneId zoneId, JobRequest jobRequest) {
        JobDetails jobDetails = new JobDetails(jobRequest);
        return scheduleRecurrently(id, jobDetails, carbonAwareCron, zoneId);
    }
}
