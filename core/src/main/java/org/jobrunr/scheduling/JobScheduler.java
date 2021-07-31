package org.jobrunr.scheduling;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.details.JobDetailsAsmGenerator;
import org.jobrunr.jobs.details.JobDetailsGenerator;
import org.jobrunr.jobs.filters.JobDefaultFilters;
import org.jobrunr.jobs.filters.JobFilter;
import org.jobrunr.jobs.filters.JobFilterUtils;
import org.jobrunr.jobs.lambdas.IocJobLambda;
import org.jobrunr.jobs.lambdas.IocJobLambdaFromStream;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.jobs.lambdas.JobLambdaFromStream;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.scheduling.cron.CronExpression;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.jobrunr.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static java.time.ZoneId.systemDefault;
import static java.util.Collections.emptyList;
import static org.jobrunr.utils.streams.StreamUtils.batchCollector;

/**
 * Provides methods for creating fire-and-forget, delayed and recurring jobs as well as to delete existing background jobs.
 *
 * @author Ronald Dehuysser
 */
public class JobScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobScheduler.class);

    private final StorageProvider storageProvider;
    private final JobDetailsGenerator jobDetailsGenerator;
    private final JobFilterUtils jobFilterUtils;
    private static final int BATCH_SIZE = 5000;

    /**
     * Creates a new JobScheduler using the provided storageProvider
     *
     * @param storageProvider the storageProvider to use
     */
    public JobScheduler(StorageProvider storageProvider) {
        this(storageProvider, emptyList());
    }

    /**
     * Creates a new JobScheduler using the provided storageProvider and the list of JobFilters that will be used for every background job
     *
     * @param storageProvider the storageProvider to use
     * @param jobFilters      list of jobFilters that will be used for every job
     */
    public JobScheduler(StorageProvider storageProvider, List<JobFilter> jobFilters) {
        this(storageProvider, new JobDetailsAsmGenerator(), jobFilters);
    }

    JobScheduler(StorageProvider storageProvider, JobDetailsGenerator jobDetailsGenerator, List<JobFilter> jobFilters) {
        if (storageProvider == null) throw new IllegalArgumentException("A JobStorageProvider is required to use the JobScheduler. Please see the documentation on how to setup a JobStorageProvider");
        this.storageProvider = storageProvider;
        this.jobDetailsGenerator = jobDetailsGenerator;
        this.jobFilterUtils = new JobFilterUtils(new JobDefaultFilters(jobFilters));
    }

    /**
     * Creates a new fire-and-forget job based on a given lambda.
     * <h5>An example:</h5>
     * <pre>{@code
     *            MyService service = new MyService();
     *            jobScheduler.enqueue(() -> service.doWork());
     *       }</pre>
     *
     * @param job the lambda which defines the fire-and-forget job
     * @return the id of the job
     */
    public JobId enqueue(JobLambda job) {
        return enqueue(null, job);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda.
     * If a job with that id already exists, JobRunr will not save it again.
     *
     * <h5>An example:</h5>
     * <pre>{@code
     *            MyService service = new MyService();
     *            jobScheduler.enqueue(id, () -> service.doWork());
     *       }</pre>
     *
     * @param id  the uuid with which to save the job
     * @param job the lambda which defines the fire-and-forget job
     * @return the id of the job
     */
    public JobId enqueue(UUID id, JobLambda job) {
        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(job);
        return enqueue(id, jobDetails);
    }

    /**
     * Creates new fire-and-forget jobs for each item in the input stream using the lambda passed as {@code jobFromStream}.
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      Stream<UUID> workStream = getWorkStream();
     *      jobScheduler.enqueue(workStream, (uuid) -> service.doWork(uuid));
     * }</pre>
     *
     * @param input         the stream of items for which to create fire-and-forget jobs
     * @param jobFromStream the lambda which defines the fire-and-forget job to create for each item in the {@code input}
     */
    public <T> void enqueue(Stream<T> input, JobLambdaFromStream<T> jobFromStream) {
        input
                .map(x -> jobDetailsGenerator.toJobDetails(x, jobFromStream))
                .map(org.jobrunr.jobs.Job::new)
                .collect(batchCollector(BATCH_SIZE, this::saveJobs));
    }

    /**
     * Creates a new fire-and-forget job based on a given lambda. The IoC container will be used to resolve {@code MyService}.
     * <h5>An example:</h5>
     * <pre>{@code
     *            jobScheduler.<MyService>enqueue(x -> x.doWork());
     *       }</pre>
     *
     * @param iocJob the lambda which defines the fire-and-forget job
     * @return the id of the job
     */
    public <S> JobId enqueue(IocJobLambda<S> iocJob) {
        return enqueue(null, iocJob);
    }

    /**
     * Creates a new fire-and-forget job based on a given lambda. The IoC container will be used to resolve {@code MyService}.
     * If a job with that id already exists, JobRunr will not save it again.
     *
     * <h5>An example:</h5>
     * <pre>{@code
     *            jobScheduler.<MyService>enqueue(id, x -> x.doWork());
     *       }</pre>
     *
     * @param id     the uuid with which to save the job
     * @param iocJob the lambda which defines the fire-and-forget job
     * @return the id of the job
     */
    public <S> JobId enqueue(UUID id, IocJobLambda<S> iocJob) {
        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(iocJob);
        return enqueue(id, jobDetails);
    }

    /**
     * Creates new fire-and-forget jobs for each item in the input stream using the lambda passed as {@code jobFromStream}. The IoC container will be used to resolve {@code MyService}.
     * <h5>An example:</h5>
     * <pre>{@code
     *      Stream<UUID> workStream = getWorkStream();
     *      jobScheduler.<MyService, UUID>enqueue(workStream, (x, uuid) -> x.doWork(uuid));
     * }</pre>
     *
     * @param input            the stream of items for which to create fire-and-forget jobs
     * @param iocJobFromStream the lambda which defines the fire-and-forget job to create for each item in the {@code input}
     */
    public <S, T> void enqueue(Stream<T> input, IocJobLambdaFromStream<S, T> iocJobFromStream) {
        input
                .map(x -> jobDetailsGenerator.toJobDetails(x, iocJobFromStream))
                .map(org.jobrunr.jobs.Job::new)
                .collect(batchCollector(BATCH_SIZE, this::saveJobs));
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time.
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      jobScheduler.schedule(ZonedDateTime.now().plusHours(5), () -> service.doWork());
     * }</pre>
     *
     * @param zonedDateTime The moment in time at which the job will be enqueued.
     * @param job           the lambda which defines the fire-and-forget job
     * @return the id of the Job
     */
    public JobId schedule(ZonedDateTime zonedDateTime, JobLambda job) {
        return schedule(null, zonedDateTime.toInstant(), job);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time.
     * If a job with that id already exists, JobRunr will not save it again.
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      jobScheduler.schedule(id, ZonedDateTime.now().plusHours(5), () -> service.doWork());
     * }</pre>
     *
     * @param id            the uuid with which to save the job
     * @param zonedDateTime The moment in time at which the job will be enqueued.
     * @param job           the lambda which defines the fire-and-forget job
     * @return the id of the Job
     */
    public JobId schedule(UUID id, ZonedDateTime zonedDateTime, JobLambda job) {
        return schedule(id, zonedDateTime.toInstant(), job);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time. The IoC container will be used to resolve {@code MyService}.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.<MyService>schedule(ZonedDateTime.now().plusHours(5), x -> x.doWork());
     * }</pre>
     *
     * @param zonedDateTime The moment in time at which the job will be enqueued.
     * @param iocJob        the lambda which defines the fire-and-forget job
     * @return the id of the Job
     */
    public <S> JobId schedule(ZonedDateTime zonedDateTime, IocJobLambda<S> iocJob) {
        return schedule(null, zonedDateTime.toInstant(), iocJob);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time. The IoC container will be used to resolve {@code MyService}.
     * If a job with that id already exists, JobRunr will not save it again.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.<MyService>schedule(id, ZonedDateTime.now().plusHours(5), x -> x.doWork());
     * }</pre>
     *
     * @param id            the uuid with which to save the job
     * @param zonedDateTime The moment in time at which the job will be enqueued.
     * @param iocJob        the lambda which defines the fire-and-forget job
     * @return the id of the Job
     */
    public <S> JobId schedule(UUID id, ZonedDateTime zonedDateTime, IocJobLambda<S> iocJob) {
        return schedule(id, zonedDateTime.toInstant(), iocJob);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time.
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      jobScheduler.schedule(OffsetDateTime.now().plusHours(5), () -> service.doWork());
     * }</pre>
     *
     * @param offsetDateTime The moment in time at which the job will be enqueued.
     * @param job            the lambda which defines the fire-and-forget job
     * @return the id of the Job
     */
    public JobId schedule(OffsetDateTime offsetDateTime, JobLambda job) {
        return schedule(null, offsetDateTime.toInstant(), job);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time.
     * If a job with that id already exists, JobRunr will not save it again.
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      jobScheduler.schedule(id, OffsetDateTime.now().plusHours(5), () -> service.doWork());
     * }</pre>
     *
     * @param id             the uuid with which to save the job
     * @param offsetDateTime The moment in time at which the job will be enqueued.
     * @param job            the lambda which defines the fire-and-forget job
     * @return the id of the Job
     */
    public JobId schedule(UUID id, OffsetDateTime offsetDateTime, JobLambda job) {
        return schedule(id, offsetDateTime.toInstant(), job);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time. The IoC container will be used to resolve {@code MyService}.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.<MyService>schedule(OffsetDateTime.now().plusHours(5), x -> x.doWork());
     * }</pre>
     *
     * @param offsetDateTime The moment in time at which the job will be enqueued.
     * @param iocJob         the lambda which defines the fire-and-forget job
     * @return the id of the Job
     */
    public <S> JobId schedule(OffsetDateTime offsetDateTime, IocJobLambda<S> iocJob) {
        return schedule(null, offsetDateTime.toInstant(), iocJob);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time. The IoC container will be used to resolve {@code MyService}.
     * If a job with that id already exists, JobRunr will not save it again.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.<MyService>schedule(id, OffsetDateTime.now().plusHours(5), x -> x.doWork());
     * }</pre>
     *
     * @param id             the uuid with which to save the job
     * @param offsetDateTime The moment in time at which the job will be enqueued.
     * @param iocJob         the lambda which defines the fire-and-forget job
     * @return the id of the Job
     */
    public <S> JobId schedule(UUID id, OffsetDateTime offsetDateTime, IocJobLambda<S> iocJob) {
        return schedule(id, offsetDateTime.toInstant(), iocJob);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time.
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      jobScheduler.schedule(LocalDateTime.now().plusHours(5), () -> service.doWork());
     * }</pre>
     *
     * @param localDateTime The moment in time at which the job will be enqueued. It will use the systemDefault ZoneId to transform it to an UTC Instant
     * @param job           the lambda which defines the fire-and-forget job
     * @return the id of the Job
     */
    public JobId schedule(LocalDateTime localDateTime, JobLambda job) {
        return schedule(localDateTime.atZone(systemDefault()).toInstant(), job);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time.
     * If a job with that id already exists, JobRunr will not save it again.
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      jobScheduler.schedule(id, LocalDateTime.now().plusHours(5), () -> service.doWork());
     * }</pre>
     *
     * @param id            the uuid with which to save the job
     * @param localDateTime The moment in time at which the job will be enqueued. It will use the systemDefault ZoneId to transform it to an UTC Instant
     * @param job           the lambda which defines the fire-and-forget job
     * @return the id of the Job
     */
    public JobId schedule(UUID id, LocalDateTime localDateTime, JobLambda job) {
        return schedule(id, localDateTime.atZone(systemDefault()).toInstant(), job);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time. The IoC container will be used to resolve {@code MyService}.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.<MyService>schedule(LocalDateTime.now().plusHours(5), x -> x.doWork());
     * }</pre>
     *
     * @param localDateTime The moment in time at which the job will be enqueued. It will use the systemDefault ZoneId to transform it to an UTC Instant
     * @param iocJob        the lambda which defines the fire-and-forget job
     * @return the id of the Job
     */
    public <S> JobId schedule(LocalDateTime localDateTime, IocJobLambda<S> iocJob) {
        return schedule(localDateTime.atZone(systemDefault()).toInstant(), iocJob);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time. The IoC container will be used to resolve {@code MyService}.
     * If a job with that id already exists, JobRunr will not save it again.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.<MyService>schedule(LocalDateTime.now().plusHours(5), x -> x.doWork());
     * }</pre>
     *
     * @param id            the uuid with which to save the job
     * @param localDateTime The moment in time at which the job will be enqueued. It will use the systemDefault ZoneId to transform it to an UTC Instant
     * @param iocJob        the lambda which defines the fire-and-forget job
     * @return the id of the Job
     */
    public <S> JobId schedule(UUID id, LocalDateTime localDateTime, IocJobLambda<S> iocJob) {
        return schedule(id, localDateTime.atZone(systemDefault()).toInstant(), iocJob);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time.
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      jobScheduler.schedule(Instant.now().plusHours(5), () -> service.doWork());
     * }</pre>
     *
     * @param instant The moment in time at which the job will be enqueued.
     * @param job     the lambda which defines the fire-and-forget job
     * @return the id of the Job
     */
    public JobId schedule(Instant instant, JobLambda job) {
        return schedule(null, instant, job);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time.
     * If a job with that id already exists, JobRunr will not save it again.
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      jobScheduler.schedule(id, Instant.now().plusHours(5), () -> service.doWork());
     * }</pre>
     *
     * @param id      the uuid with which to save the job
     * @param instant The moment in time at which the job will be enqueued.
     * @param job     the lambda which defines the fire-and-forget job
     * @return the id of the Job
     */
    public JobId schedule(UUID id, Instant instant, JobLambda job) {
        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(job);
        return schedule(id, instant, jobDetails);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time. The IoC container will be used to resolve {@code MyService}.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.<MyService>schedule(Instant.now().plusHours(5), x -> x.doWork());
     * }</pre>
     *
     * @param instant The moment in time at which the job will be enqueued.
     * @param iocJob  the lambda which defines the fire-and-forget job
     * @return the id of the Job
     */
    public <S> JobId schedule(Instant instant, IocJobLambda<S> iocJob) {
        return schedule(null, instant, iocJob);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time. The IoC container will be used to resolve {@code MyService}.
     * If a job with that id already exists, JobRunr will not save it again.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.<MyService>schedule(id, Instant.now().plusHours(5), x -> x.doWork());
     * }</pre>
     *
     * @param id      the uuid with which to save the job
     * @param instant The moment in time at which the job will be enqueued.
     * @param iocJob  the lambda which defines the fire-and-forget job
     * @return the id of the Job
     */
    public <S> JobId schedule(UUID id, Instant instant, IocJobLambda<S> iocJob) {
        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(iocJob);
        return schedule(id, instant, jobDetails);
    }

    /**
     * @see #delete(UUID)
     */
    public void delete(JobId jobId) {
        this.delete(jobId.asUUID());
    }

    /**
     * Deletes a job and sets it's state to DELETED. If the job is being processed, it will be interrupted.
     *
     * @param id the id of the job
     */
    public void delete(UUID id) {
        final Job jobToDelete = storageProvider.getJobById(id);
        jobToDelete.delete();
        jobFilterUtils.runOnStateElectionFilter(jobToDelete);
        final Job deletedJob = storageProvider.save(jobToDelete);
        jobFilterUtils.runOnStateAppliedFilters(deletedJob);
        LOGGER.debug("Deleted Job with id {}", deletedJob.getId());
    }

    /**
     * Creates a new recurring job based on the given lambda and the given cron expression. The jobs will be scheduled using the systemDefault timezone.
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      jobScheduler.scheduleRecurrently(Cron.daily(), () -> service.doWork());
     * }</pre>
     *
     * @param cron The cron expression defining when to run this recurring job
     * @param job  the lambda which defines the fire-and-forget job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    public String scheduleRecurrently(String cron, JobLambda job) {
        return scheduleRecurrently(null, cron, job);
    }

    /**
     * Creates a new recurring job based on the given cron expression and the given lambda. The IoC container will be used to resolve {@code MyService}. The jobs will be scheduled using the systemDefault timezone.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.<MyService>scheduleRecurrently(Cron.daily(), x -> x.doWork());
     * }</pre>
     *
     * @param cron   The cron expression defining when to run this recurring job
     * @param iocJob the lambda which defines the fire-and-forget job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    public <S> String scheduleRecurrently(String cron, IocJobLambda<S> iocJob) {
        return scheduleRecurrently(null, cron, iocJob);
    }

    /**
     * Creates a new recurring job based on the given id, cron expression and lambda. The jobs will be scheduled using the systemDefault timezone
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      jobScheduler.scheduleRecurrently("my-recurring-job", Cron.daily(), () -> service.doWork());
     * }</pre>
     *
     * @param id   the id of this recurring job which can be used to alter or delete it
     * @param cron The cron expression defining when to run this recurring job
     * @param job  the lambda which defines the fire-and-forget job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    public String scheduleRecurrently(String id, String cron, JobLambda job) {
        return scheduleRecurrently(id, cron, systemDefault(), job);
    }

    /**
     * Creates a new recurring job based on the given id, cron expression and lambda. The IoC container will be used to resolve {@code MyService}. The jobs will be scheduled using the systemDefault timezone
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.<MyService>scheduleRecurrently("my-recurring-job", Cron.daily()),  x -> x.doWork();
     * }</pre>
     *
     * @param id     the id of this recurring job which can be used to alter or delete it
     * @param cron   The cron expression defining when to run this recurring job
     * @param iocJob the lambda which defines the fire-and-forget job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    public <S> String scheduleRecurrently(String id, String cron, IocJobLambda<S> iocJob) {
        return scheduleRecurrently(id, cron, systemDefault(), iocJob);
    }

    /**
     * Creates a new recurring job based on the given id, cron expression, {@code ZoneId} and lambda.
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      jobScheduler.scheduleRecurrently("my-recurring-job", Cron.daily(), ZoneId.of("Europe/Brussels"), () -> service.doWork());
     * }</pre>
     *
     * @param id     the id of this recurring job which can be used to alter or delete it
     * @param cron   The cron expression defining when to run this recurring job
     * @param zoneId The zoneId (timezone) of when to run this recurring job
     * @param job    the lambda which defines the fire-and-forget job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    public String scheduleRecurrently(String id, String cron, ZoneId zoneId, JobLambda job) {
        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(job);
        return scheduleRecurrently(id, jobDetails, CronExpression.create(cron), zoneId);
    }

    /**
     * Creates a new recurring job based on the given id, cron expression, {@code ZoneId} and lambda. The IoC container will be used to resolve {@code MyService}.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.<MyService>scheduleRecurrently("my-recurring-job", Cron.daily(), ZoneId.of("Europe/Brussels"), x -> x.doWork());
     * }</pre>
     *
     * @param id     the id of this recurring job which can be used to alter or delete it
     * @param cron   The cron expression defining when to run this recurring job
     * @param zoneId The zoneId (timezone) of when to run this recurring job
     * @param iocJob the lambda which defines the fire-and-forget job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    public <S> String scheduleRecurrently(String id, String cron, ZoneId zoneId, IocJobLambda<S> iocJob) {
        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(iocJob);
        return scheduleRecurrently(id, jobDetails, CronExpression.create(cron), zoneId);
    }

    /**
     * Deletes the recurring job based on the given id.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.delete("my-recurring-job"));
     * }</pre>
     *
     * @param id the id of the recurring job to delete
     */
    public void delete(String id) {
        this.storageProvider.deleteRecurringJob(id);
    }

    /**
     * Utility method to register the shutdown of JobRunr in various containers - it is even automatically called by Spring Framework.
     * Note that this will stop the BackgroundJobServer, the Dashboard and the StorageProvider. JobProcessing will stop and enqueueing new jobs will fail.
     */
    public void shutdown() {
        JobRunr.destroy();
    }

    JobId enqueue(UUID id, JobDetails jobDetails) {
        return saveJob(new Job(id, jobDetails));
    }

    JobId schedule(UUID id, Instant scheduleAt, JobDetails jobDetails) {
        return saveJob(new Job(id, jobDetails, new ScheduledState(scheduleAt)));
    }

    String scheduleRecurrently(String id, JobDetails jobDetails, CronExpression cronExpression, ZoneId zoneId) {
        final RecurringJob recurringJob = new RecurringJob(id, jobDetails, cronExpression, zoneId);
        jobFilterUtils.runOnCreatingFilter(recurringJob);
        RecurringJob savedRecurringJob = this.storageProvider.saveRecurringJob(recurringJob);
        jobFilterUtils.runOnCreatedFilter(recurringJob);
        return savedRecurringJob.getId();
    }

    JobId saveJob(Job job) {
        try {
            jobFilterUtils.runOnCreatingFilter(job);
            Job savedJob = this.storageProvider.save(job);
            jobFilterUtils.runOnCreatedFilter(savedJob);
            LOGGER.debug("Created Job with id {}", job.getId());
        } catch (ConcurrentJobModificationException e) {
            LOGGER.info("Skipped Job with id {} as it already exists", job.getId());
        }
        return new JobId(job.getId());
    }

    List<Job> saveJobs(List<Job> jobs) {
        jobFilterUtils.runOnCreatingFilter(jobs);
        final List<Job> savedJobs = this.storageProvider.save(jobs);
        jobFilterUtils.runOnCreatedFilter(savedJobs);
        return savedJobs;
    }
}
