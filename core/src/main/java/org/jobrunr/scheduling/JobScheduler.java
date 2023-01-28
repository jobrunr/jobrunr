package org.jobrunr.scheduling;

import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.details.JobDetailsAsmGenerator;
import org.jobrunr.jobs.details.JobDetailsGenerator;
import org.jobrunr.jobs.filters.JobFilter;
import org.jobrunr.jobs.lambdas.IocJobLambda;
import org.jobrunr.jobs.lambdas.IocJobLambdaFromStream;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.jobs.lambdas.JobLambdaFromStream;
import org.jobrunr.scheduling.cron.CronExpression;
import org.jobrunr.scheduling.interval.Interval;
import org.jobrunr.storage.StorageProvider;

import java.time.*;
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
 * This {@code JobScheduler} allows to schedule jobs by means of a Java 8 lambda which is analyzed.
 *
 * @author Ronald Dehuysser
 */
public class JobScheduler extends AbstractJobScheduler {

    private final JobDetailsGenerator jobDetailsGenerator;

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

    public JobScheduler(StorageProvider storageProvider, JobDetailsGenerator jobDetailsGenerator, List<JobFilter> jobFilters) {
        super(storageProvider, jobFilters);
        if (jobDetailsGenerator == null)
            throw new IllegalArgumentException("A JobDetailsGenerator is required to use the JobScheduler.");
        this.jobDetailsGenerator = jobDetailsGenerator;
        BackgroundJob.setJobScheduler(this);
    }


    /**
     * Creates a new {@link org.jobrunr.jobs.Job} using a {@link JobBuilder} that can be enqueued or scheduled and provides an alternative to the job annotation.
     * @param jobBuilder the jobBuilder with all the details of the job
     * @return the id of the job
     */
    @Override
    public JobId create(JobBuilder jobBuilder) {
        return saveJob(jobBuilder.build(jobDetailsGenerator));
    }

    /**
     * Creates a new {@link org.jobrunr.jobs.Job} for each {@link JobBuilder} and provides an alternative to the job annotation.
     *
     * @param jobBuilderStream the jobBuilders for which to create jobs.
     */
    @Override
    public void create(Stream<JobBuilder> jobBuilderStream) {
        jobBuilderStream
                .map(jobBuilder -> jobBuilder.build(jobDetailsGenerator))
                .collect(batchCollector(BATCH_SIZE, this::saveJobs));
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
     * @param zonedDateTime the moment in time at which the job will be enqueued.
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
     * @param zonedDateTime the moment in time at which the job will be enqueued.
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
     * @param zonedDateTime the moment in time at which the job will be enqueued.
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
     * @param zonedDateTime the moment in time at which the job will be enqueued.
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
     * @param localDateTime the moment in time at which the job will be enqueued. It will use the systemDefault ZoneId to transform it to an UTC Instant
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
     * @param localDateTime the moment in time at which the job will be enqueued. It will use the systemDefault ZoneId to transform it to an UTC Instant
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
     * @param localDateTime the moment in time at which the job will be enqueued. It will use the systemDefault ZoneId to transform it to an UTC Instant
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
     * @param localDateTime the moment in time at which the job will be enqueued. It will use the systemDefault ZoneId to transform it to an UTC Instant
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
     * @param instant the moment in time at which the job will be enqueued.
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
     * @param instant the moment in time at which the job will be enqueued.
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
     * @param instant the moment in time at which the job will be enqueued.
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
     * @param instant the moment in time at which the job will be enqueued.
     * @param iocJob  the lambda which defines the fire-and-forget job
     * @return the id of the Job
     */
    public <S> JobId schedule(UUID id, Instant instant, IocJobLambda<S> iocJob) {
        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(iocJob);
        return schedule(id, instant, jobDetails);
    }

    /**
     * Creates a new or alters the existing recurring job based on the given RecurringJobBuilder (using id, cron expression and lambda).
     * If no zoneId is set on the builder the jobs will be scheduled using the systemDefault timezone.
     * <h5>An example:</h5>
     *
     <pre>{@code
     *      jobScheduler.createRecurrently(aRecurringJob()
     *                                          .withCron("* * 0 * * *")
     *                                          .withDetails(() -> service.doWork());
     * }</pre>
     *
     * @param recurringJobBuilder the builder describing your recurring job.
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    @Override
    public String createRecurrently(RecurringJobBuilder recurringJobBuilder) {
        RecurringJob recurringJob = recurringJobBuilder.build(jobDetailsGenerator);
        return this.scheduleRecurrently(recurringJob);
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
     * @param job  the lambda which defines the recurring job
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
     * @param iocJob the lambda which defines the recurring job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    public <S> String scheduleRecurrently(String cron, IocJobLambda<S> iocJob) {
        return scheduleRecurrently(null, cron, iocJob);
    }

    /**
     * Creates a new or alters the existing recurring job based on the given id, cron expression and lambda. The jobs will be scheduled using the systemDefault timezone
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      jobScheduler.scheduleRecurrently("my-recurring-job", Cron.daily(), () -> service.doWork());
     * }</pre>
     *
     * @param id   the id of this recurring job which can be used to alter or delete it
     * @param cron The cron expression defining when to run this recurring job
     * @param job  the lambda which defines the recurring job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    public String scheduleRecurrently(String id, String cron, JobLambda job) {
        return scheduleRecurrently(id, cron, systemDefault(), job);
    }

    /**
     * Creates a new or alters the existing recurring job based on the given id, cron expression and lambda. The IoC container will be used to resolve {@code MyService}. The jobs will be scheduled using the systemDefault timezone
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.<MyService>scheduleRecurrently("my-recurring-job", Cron.daily()),  x -> x.doWork();
     * }</pre>
     *
     * @param id     the id of this recurring job which can be used to alter or delete it
     * @param cron   The cron expression defining when to run this recurring job
     * @param iocJob the lambda which defines the recurring job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    public <S> String scheduleRecurrently(String id, String cron, IocJobLambda<S> iocJob) {
        return scheduleRecurrently(id, cron, systemDefault(), iocJob);
    }

    /**
     * Creates a new or alters the existing recurring job based on the given id, cron expression, {@code ZoneId} and lambda.
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      jobScheduler.scheduleRecurrently("my-recurring-job", Cron.daily(), ZoneId.of("Europe/Brussels"), () -> service.doWork());
     * }</pre>
     *
     * @param id     the id of this recurring job which can be used to alter or delete it
     * @param cron   The cron expression defining when to run this recurring job
     * @param zoneId The zoneId (timezone) of when to run this recurring job
     * @param job    the lambda which defines the recurring job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    public String scheduleRecurrently(String id, String cron, ZoneId zoneId, JobLambda job) {
        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(job);
        return scheduleRecurrently(id, jobDetails, CronExpression.create(cron), zoneId);
    }

    /**
     * Creates a new or alters the existing recurring job based on the given id, cron expression, {@code ZoneId} and lambda. The IoC container will be used to resolve {@code MyService}.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.<MyService>scheduleRecurrently("my-recurring-job", Cron.daily(), ZoneId.of("Europe/Brussels"), x -> x.doWork());
     * }</pre>
     *
     * @param id     the id of this recurring job which can be used to alter or delete it
     * @param cron   The cron expression defining when to run this recurring job
     * @param zoneId The zoneId (timezone) of when to run this recurring job
     * @param iocJob the lambda which defines the recurring job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    public <S> String scheduleRecurrently(String id, String cron, ZoneId zoneId, IocJobLambda<S> iocJob) {
        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(iocJob);
        return scheduleRecurrently(id, jobDetails, CronExpression.create(cron), zoneId);
    }

    /**
     * Creates a new recurring job based on the given duration and the given lambda. The first run of this recurring job will happen after the given duration unless your duration is smaller or equal than your backgroundJobServer pollInterval.
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      BackgroundJob.scheduleRecurrently(Duration.parse("P5D"), () -> service.doWork());
     * }</pre>
     *
     * @param duration the duration defining the time between each instance of this recurring job.
     * @param job      the lambda which defines the recurring job
     * @return the id of this recurring job which can be used to alter or delete it
     */
    public String scheduleRecurrently(Duration duration, JobLambda job) {
        return scheduleRecurrently(null, duration, job);
    }

    /**
     * Creates a new recurring job based on the given duration and the given lambda. The IoC container will be used to resolve {@code MyService}. The first run of this recurring job will happen after the given duration unless your duration is smaller or equal than your backgroundJobServer pollInterval.
     * <h5>An example:</h5>
     * <pre>{@code
     *      BackgroundJob.<MyService>scheduleRecurrently(Duration.parse("P5D"), x -> x.doWork());
     * }</pre>
     *
     * @param duration the duration defining the time between each instance of this recurring job
     * @param iocJob   the lambda which defines the recurring job
     * @return the id of this recurring job which can be used to alter or delete it
     */
    public <S> String scheduleRecurrently(Duration duration, IocJobLambda<S> iocJob) {
        return scheduleRecurrently(null, duration, iocJob);
    }

    /**
     * Creates a new or alters the existing recurring job based on the given id, duration and lambda. The first run of this recurring job will happen after the given duration unless your duration is smaller or equal than your backgroundJobServer pollInterval.
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      BackgroundJob.scheduleRecurrently("my-recurring-job", Duration.parse("P5D"), () -> service.doWork());
     * }</pre>
     *
     * @param id       the id of this recurring job which can be used to alter or delete it
     * @param duration the duration defining the time between each instance of this recurring job
     * @param job      the lambda which defines the recurring job
     * @return the id of this recurring job which can be used to alter or delete it
     */
    public String scheduleRecurrently(String id, Duration duration, JobLambda job) {
        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(job);
        return scheduleRecurrently(id, jobDetails, new Interval(duration), systemDefault());
    }

    /**
     * Creates a new or alters the existing recurring job based on the given id, duration and lambda. The IoC container will be used to resolve {@code MyService}. The first run of this recurring job will happen after the given duration unless your duration is smaller or equal than your backgroundJobServer pollInterval.
     * <h5>An example:</h5>
     * <pre>{@code
     *      BackgroundJob.<MyService>scheduleRecurrently("my-recurring-job", Duration.parse("P5D"), x -> x.doWork());
     * }</pre>
     *
     * @param id       the id of this recurring job which can be used to alter or delete it
     * @param duration the duration defining the time between each instance of this recurring job
     * @param iocJob   the lambda which defines the recurring job
     * @return the id of this recurring job which can be used to alter or delete it
     */
    public <S> String scheduleRecurrently(String id, Duration duration, IocJobLambda<S> iocJob) {
        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(iocJob);
        return scheduleRecurrently(id, jobDetails, new Interval(duration), systemDefault());
    }
}
