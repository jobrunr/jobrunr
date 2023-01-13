package org.jobrunr.scheduling;

import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.lambdas.IocJobLambda;
import org.jobrunr.jobs.lambdas.IocJobLambdaFromStream;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.jobs.lambdas.JobLambdaFromStream;

import java.time.*;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Provides static methods for creating fire-and-forget, delayed and recurring jobs as well as to delete existing background jobs.
 * If you prefer not to use a static accessor, you can inject the {@link JobScheduler} which exposes the same methods.
 *
 * @author Ronald Dehuysser
 */
public class BackgroundJob {

    BackgroundJob() {
    }

    private static JobScheduler jobScheduler;

    /**
     * Creates a new {@link org.jobrunr.jobs.Job} using a {@link JobBuilder} that can be enqueued or scheduled and provides an alternative to the job annotation.
     * @param jobBuilder the jobBuilder with all the details of the job
     * @return the id of the job
     */
    public static JobId create(JobBuilder jobBuilder) {
        return jobScheduler.create(jobBuilder);
    }

    /**
     * Creates a new {@link org.jobrunr.jobs.Job} for each {@link JobBuilder} and provides an alternative to the job annotation.
     *
     * @param jobBuilderStream the jobBuilders for which to create jobs.
     */
    public static void create(Stream<JobBuilder> jobBuilderStream) {
        jobScheduler.create(jobBuilderStream);
    }

    /**
     * Creates a new fire-and-forget job based on a given lambda.
     * <h5>An example:</h5>
     * <pre>{@code
     *            MyService service = new MyService();
     *            BackgroundJob.enqueue(() -> service.doWork());
     *       }</pre>
     *
     * @param job the lambda which defines the fire-and-forget job
     * @return the id of the job
     */
    public static JobId enqueue(JobLambda job) {
        verifyJobScheduler();
        return jobScheduler.enqueue(job);
    }

    /**
     * Creates a new fire-and-forget job based on a given lambda.
     * If a job with that id already exists, JobRunr will not save it again.
     *
     * <h5>An example:</h5>
     * <pre>{@code
     *            MyService service = new MyService();
     *            BackgroundJob.enqueue(id, () -> service.doWork());
     *       }</pre>
     *
     * @param id  the uuid with which to save the job
     * @param job the lambda which defines the fire-and-forget job
     * @return the id of the job
     */
    public static JobId enqueue(UUID id, JobLambda job) {
        verifyJobScheduler();
        return jobScheduler.enqueue(id, job);
    }

    /**
     * Creates new fire-and-forget jobs for each item in the input stream using the lambda passed as {@code jobFromStream}.
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      Stream<UUID> workStream = getWorkStream();
     *      BackgroundJob.enqueue(workStream, (uuid) -> service.doWork(uuid));
     * }</pre>
     *
     * @param input         the stream of items for which to create fire-and-forget jobs
     * @param jobFromStream the lambda which defines the fire-and-forget job to create for each item in the {@code input}
     */
    public static <T> void enqueue(Stream<T> input, JobLambdaFromStream<T> jobFromStream) {
        verifyJobScheduler();
        jobScheduler.enqueue(input, jobFromStream);
    }

    /**
     * Creates a new fire-and-forget job based on a given lambda. The IoC container will be used to resolve {@code MyService}.
     * <h5>An example:</h5>
     * <pre>{@code
     *            BackgroundJob.<MyService>enqueue(x -> x.doWork());
     *       }</pre>
     *
     * @param iocJob the lambda which defines the fire-and-forget job
     * @return the id of the job
     */
    public static <S> JobId enqueue(IocJobLambda<S> iocJob) {
        verifyJobScheduler();
        return jobScheduler.enqueue(iocJob);
    }

    /**
     * Creates a new fire-and-forget job based on a given lambda. The IoC container will be used to resolve {@code MyService}.
     * If a job with that id already exists, JobRunr will not save it again.
     * <h5>An example:</h5>
     * <pre>{@code
     *            BackgroundJob.<MyService>enqueue(id, x -> x.doWork());
     *       }</pre>
     *
     * @param id     the uuid with which to save the job
     * @param iocJob the lambda which defines the fire-and-forget job
     * @return the id of the job
     */
    public static <S> JobId enqueue(UUID id, IocJobLambda<S> iocJob) {
        verifyJobScheduler();
        return jobScheduler.enqueue(id, iocJob);
    }

    /**
     * Creates new fire-and-forget jobs for each item in the input stream using the lambda passed as {@code jobFromStream}. The IoC container will be used to resolve {@code MyService}.
     * <h5>An example:</h5>
     * <pre>{@code
     *      Stream<UUID> workStream = getWorkStream();
     *      BackgroundJob.<MyService, UUID>enqueue(workStream, (x, uuid) -> x.doWork(uuid));
     * }</pre>
     *
     * @param input            the stream of items for which to create fire-and-forget jobs
     * @param iocJobFromStream the lambda which defines the fire-and-forget job to create for each item in the {@code input}
     */
    public static <S, T> void enqueue(Stream<T> input, IocJobLambdaFromStream<S, T> iocJobFromStream) {
        verifyJobScheduler();
        jobScheduler.enqueue(input, iocJobFromStream);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time.
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      BackgroundJob.schedule(ZonedDateTime.now().plusHours(5), () -> service.doWork());
     * }</pre>
     *
     * @param zonedDateTime The moment in time at which the job will be enqueued.
     * @param job           the lambda which defines the fire-and-forget job
     * @return the id of the Job
     */
    public static JobId schedule(ZonedDateTime zonedDateTime, JobLambda job) {
        verifyJobScheduler();
        return jobScheduler.schedule(zonedDateTime, job);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time.
     * If a job with that id already exists, JobRunr will not save it again.
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      BackgroundJob.schedule(id, ZonedDateTime.now().plusHours(5), () -> service.doWork());
     * }</pre>
     *
     * @param id            the uuid with which to save the job
     * @param zonedDateTime The moment in time at which the job will be enqueued.
     * @param job           the lambda which defines the fire-and-forget job
     * @return the id of the Job
     */
    public static JobId schedule(UUID id, ZonedDateTime zonedDateTime, JobLambda job) {
        verifyJobScheduler();
        return jobScheduler.schedule(id, zonedDateTime, job);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time. The IoC container will be used to resolve {@code MyService}.
     * <h5>An example:</h5>
     * <pre>{@code
     *      BackgroundJob.<MyService>schedule(ZonedDateTime.now().plusHours(5), x -> x.doWork());
     * }</pre>
     *
     * @param zonedDateTime The moment in time at which the job will be enqueued.
     * @param iocJob        the lambda which defines the fire-and-forget job
     * @return the id of the Job
     */
    public static <S> JobId schedule(ZonedDateTime zonedDateTime, IocJobLambda<S> iocJob) {
        verifyJobScheduler();
        return jobScheduler.schedule(zonedDateTime, iocJob);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time. The IoC container will be used to resolve {@code MyService}.
     * If a job with that id already exists, JobRunr will not save it again.
     * <h5>An example:</h5>
     * <pre>{@code
     *      BackgroundJob.<MyService>schedule(id, ZonedDateTime.now().plusHours(5), x -> x.doWork());
     * }</pre>
     *
     * @param id            the uuid with which to save the job
     * @param zonedDateTime The moment in time at which the job will be enqueued.
     * @param iocJob        the lambda which defines the fire-and-forget job
     * @return the id of the Job
     */
    public static <S> JobId schedule(UUID id, ZonedDateTime zonedDateTime, IocJobLambda<S> iocJob) {
        verifyJobScheduler();
        return jobScheduler.schedule(id, zonedDateTime, iocJob);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time.
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      BackgroundJob.schedule(OffsetDateTime.now().plusHours(5), () -> service.doWork());
     * }</pre>
     *
     * @param offsetDateTime The moment in time at which the job will be enqueued.
     * @param job            the lambda which defines the fire-and-forget job
     * @return the id of the Job
     */
    public static JobId schedule(OffsetDateTime offsetDateTime, JobLambda job) {
        verifyJobScheduler();
        return jobScheduler.schedule(offsetDateTime, job);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time.
     * If a job with that id already exists, JobRunr will not save it again.
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      BackgroundJob.schedule(id, OffsetDateTime.now().plusHours(5), () -> service.doWork());
     * }</pre>
     *
     * @param id             the uuid with which to save the job
     * @param offsetDateTime The moment in time at which the job will be enqueued.
     * @param job            the lambda which defines the fire-and-forget job
     * @return the id of the Job
     */
    public static JobId schedule(UUID id, OffsetDateTime offsetDateTime, JobLambda job) {
        verifyJobScheduler();
        return jobScheduler.schedule(id, offsetDateTime, job);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time. The IoC container will be used to resolve {@code MyService}.
     * <h5>An example:</h5>
     * <pre>{@code
     *      BackgroundJob.<MyService>schedule(id, OffsetDateTime.now().plusHours(5), x -> x.doWork());
     * }</pre>
     *
     * @param offsetDateTime The moment in time at which the job will be enqueued.
     * @param iocJob         the lambda which defines the fire-and-forget job
     * @return the id of the Job
     */
    public static <S> JobId schedule(OffsetDateTime offsetDateTime, IocJobLambda<S> iocJob) {
        verifyJobScheduler();
        return jobScheduler.schedule(offsetDateTime, iocJob);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time. The IoC container will be used to resolve {@code MyService}.
     * If a job with that id already exists, JobRunr will not save it again.
     * <h5>An example:</h5>
     * <pre>{@code
     *      BackgroundJob.<MyService>schedule(id, OffsetDateTime.now().plusHours(5), x -> x.doWork());
     * }</pre>
     *
     * @param id             the uuid with which to save the job
     * @param offsetDateTime The moment in time at which the job will be enqueued.
     * @param iocJob         the lambda which defines the fire-and-forget job
     * @return the id of the Job
     */
    public static <S> JobId schedule(UUID id, OffsetDateTime offsetDateTime, IocJobLambda<S> iocJob) {
        verifyJobScheduler();
        return jobScheduler.schedule(id, offsetDateTime, iocJob);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time.
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      BackgroundJob.schedule(LocalDateTime.now().plusHours(5), () -> service.doWork());
     * }</pre>
     *
     * @param localDateTime The moment in time at which the job will be enqueued. It will use the systemDefault ZoneId to transform it to an UTC Instant
     * @param job           the lambda which defines the fire-and-forget job
     * @return the id of the Job
     */
    public static JobId schedule(LocalDateTime localDateTime, JobLambda job) {
        verifyJobScheduler();
        return jobScheduler.schedule(localDateTime, job);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time.
     * If a job with that id already exists, JobRunr will not save it again.
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      BackgroundJob.schedule(id, LocalDateTime.now().plusHours(5), () -> service.doWork());
     * }</pre>
     *
     * @param id            the uuid with which to save the job
     * @param localDateTime The moment in time at which the job will be enqueued. It will use the systemDefault ZoneId to transform it to an UTC Instant
     * @param job           the lambda which defines the fire-and-forget job
     * @return the id of the Job
     */
    public static JobId schedule(UUID id, LocalDateTime localDateTime, JobLambda job) {
        verifyJobScheduler();
        return jobScheduler.schedule(id, localDateTime, job);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time. The IoC container will be used to resolve {@code MyService}.
     * <h5>An example:</h5>
     * <pre>{@code
     *      BackgroundJob.<MyService>schedule(LocalDateTime.now().plusHours(5), x -> x.doWork());
     * }</pre>
     *
     * @param localDateTime The moment in time at which the job will be enqueued. It will use the systemDefault ZoneId to transform it to an UTC Instant
     * @param iocJob        the lambda which defines the fire-and-forget job
     * @return the id of the Job
     */
    public static <S> JobId schedule(LocalDateTime localDateTime, IocJobLambda<S> iocJob) {
        verifyJobScheduler();
        return jobScheduler.schedule(localDateTime, iocJob);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time. The IoC container will be used to resolve {@code MyService}.
     * If a job with that id already exists, JobRunr will not save it again.
     * <h5>An example:</h5>
     * <pre>{@code
     *      BackgroundJob.<MyService>schedule(id, LocalDateTime.now().plusHours(5), x -> x.doWork());
     * }</pre>
     *
     * @param id            the uuid with which to save the job
     * @param localDateTime The moment in time at which the job will be enqueued. It will use the systemDefault ZoneId to transform it to an UTC Instant
     * @param iocJob        the lambda which defines the fire-and-forget job
     * @return the id of the Job
     */
    public static <S> JobId schedule(UUID id, LocalDateTime localDateTime, IocJobLambda<S> iocJob) {
        verifyJobScheduler();
        return jobScheduler.schedule(id, localDateTime, iocJob);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time.
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      BackgroundJob.schedule(Instant.now().plusHours(5), () -> service.doWork());
     * }</pre>
     *
     * @param instant The moment in time at which the job will be enqueued.
     * @param job     the lambda which defines the fire-and-forget job
     * @return the id of the Job
     */
    public static JobId schedule(Instant instant, JobLambda job) {
        verifyJobScheduler();
        return jobScheduler.schedule(instant, job);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time.
     * If a job with that id already exists, JobRunr will not save it again.
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      BackgroundJob.schedule(id, Instant.now().plusHours(5), () -> service.doWork());
     * }</pre>
     *
     * @param id      the uuid with which to save the job
     * @param instant The moment in time at which the job will be enqueued.
     * @param job     the lambda which defines the fire-and-forget job
     * @return the id of the Job
     */
    public static JobId schedule(UUID id, Instant instant, JobLambda job) {
        verifyJobScheduler();
        return jobScheduler.schedule(id, instant, job);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time. The IoC container will be used to resolve {@code MyService}.
     *
     * <h5>An example:</h5>
     * <pre>{@code
     *      BackgroundJob.<MyService>schedule(Instant.now().plusHours(5), x -> x.doWork());
     * }</pre>
     *
     * @param instant The moment in time at which the job will be enqueued.
     * @param iocJob  the lambda which defines the fire-and-forget job
     * @return the id of the Job
     */
    public static <S> JobId schedule(Instant instant, IocJobLambda<S> iocJob) {
        verifyJobScheduler();
        return jobScheduler.schedule(instant, iocJob);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time. The IoC container will be used to resolve {@code MyService}.
     * If a job with that id already exists, JobRunr will not save it again.
     * <h5>An example:</h5>
     * <pre>{@code
     *      BackgroundJob.<MyService>schedule(id, Instant.now().plusHours(5), x -> x.doWork());
     * }</pre>
     *
     * @param id      the uuid with which to save the job
     * @param instant The moment in time at which the job will be enqueued.
     * @param iocJob  the lambda which defines the fire-and-forget job
     * @return the id of the Job
     */
    public static <S> JobId schedule(UUID id, Instant instant, IocJobLambda<S> iocJob) {
        verifyJobScheduler();
        return jobScheduler.schedule(id, instant, iocJob);
    }

    /**
     * Deletes a job and sets its state to DELETED. If the job is being processed, it will be interrupted.
     *
     * @param id the id of the job
     */
    public static void delete(UUID id) {
        verifyJobScheduler();
        jobScheduler.delete(id);
    }

    /**
     * @see #delete(UUID)
     */
    public static void delete(JobId jobId) {
        delete(jobId.asUUID());
    }

    /**
     * Creates a new recurring job based on the given cron expression and the given lambda. The jobs will be scheduled using the systemDefault timezone.
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      BackgroundJob.scheduleRecurrently(Cron.daily(), () -> service.doWork());
     * }</pre>
     *
     * @param cron The cron expression defining when to run this recurring job
     * @param job  the lambda which defines the fire-and-forget job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    public static String scheduleRecurrently(String cron, JobLambda job) {
        verifyJobScheduler();
        return jobScheduler.scheduleRecurrently(cron, job);
    }

    /**
     * Creates a new recurring job based on the given cron expression and the given lambda. The IoC container will be used to resolve {@code MyService}. The jobs will be scheduled using the systemDefault timezone.
     * <h5>An example:</h5>
     * <pre>{@code
     *      BackgroundJob.<MyService>scheduleRecurrently(Cron.daily(), x -> x.doWork());
     * }</pre>
     *
     * @param cron   The cron expression defining when to run this recurring job
     * @param iocJob the lambda which defines the fire-and-forget job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    public static <S> String scheduleRecurrently(String cron, IocJobLambda<S> iocJob) {
        verifyJobScheduler();
        return jobScheduler.scheduleRecurrently(cron, iocJob);
    }

    /**
     * Creates a new or alters the existing recurring job based on the given id, cron expression and lambda. The jobs will be scheduled using the systemDefault timezone
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      BackgroundJob.scheduleRecurrently("my-recurring-job", Cron.daily(), () -> service.doWork());
     * }</pre>
     *
     * @param id   the id of this recurring job which can be used to alter or delete it
     * @param cron The cron expression defining when to run this recurring job
     * @param job  the lambda which defines the fire-and-forget job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    public static String scheduleRecurrently(String id, String cron, JobLambda job) {
        verifyJobScheduler();
        return jobScheduler.scheduleRecurrently(id, cron, job);
    }

    /**
     * Creates a new or alters the existing recurring job based on the given id, cron expression and lambda. The IoC container will be used to resolve {@code MyService}. The jobs will be scheduled using the systemDefault timezone
     * <h5>An example:</h5>
     * <pre>{@code
     *      BackgroundJob.<MyService>scheduleRecurrently("my-recurring-job", Cron.daily(), x -> x.doWork());
     * }</pre>
     *
     * @param id     the id of this recurring job which can be used to alter or delete it
     * @param cron   The cron expression defining when to run this recurring job
     * @param iocJob the lambda which defines the fire-and-forget job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    public static <S> String scheduleRecurrently(String id, String cron, IocJobLambda<S> iocJob) {
        verifyJobScheduler();
        return jobScheduler.scheduleRecurrently(id, cron, iocJob);
    }

    /**
     * Creates a new or alters the existing recurring job based on the given id, cron expression, {@code ZoneId} and lambda.
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      BackgroundJob.scheduleRecurrently("my-recurring-job", Cron.daily(), ZoneId.of("Europe/Brussels"), () -> service.doWork());
     * }</pre>
     *
     * @param id     the id of this recurring job which can be used to alter or delete it
     * @param cron   The cron expression defining when to run this recurring job
     * @param zoneId The zoneId (timezone) of when to run this recurring job
     * @param job    the lambda which defines the fire-and-forget job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    public static String scheduleRecurrently(String id, String cron, ZoneId zoneId, JobLambda job) {
        verifyJobScheduler();
        return jobScheduler.scheduleRecurrently(id, cron, zoneId, job);
    }

    /**
     * Creates a new or alters the existing recurring job based on the given id, cron expression, {@code ZoneId} and lambda. The IoC container will be used to resolve {@code MyService}.
     * <h5>An example:</h5>
     * <pre>{@code
     *      BackgroundJob.<MyService>scheduleRecurrently("my-recurring-job", Cron.daily(), ZoneId.of("Europe/Brussels"), x -> x.doWork());
     * }</pre>
     *
     * @param id     the id of this recurring job which can be used to alter or delete it
     * @param cron   The cron expression defining when to run this recurring job
     * @param zoneId The zoneId (timezone) of when to run this recurring job
     * @param iocJob the lambda which defines the fire-and-forget job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    public static <S> String scheduleRecurrently(String id, String cron, ZoneId zoneId, IocJobLambda<S> iocJob) {
        verifyJobScheduler();
        return jobScheduler.scheduleRecurrently(id, cron, zoneId, iocJob);
    }

    /**
     * Creates a new recurring job based on the given duration and the given lambda. The first run of this recurring job will happen after the given duration unless your duration is smaller or equal than your backgroundJobServer pollInterval.
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      BackgroundJob.scheduleRecurrently(Duration.parse("P5D"), () -> service.doWork());
     * }</pre>
     *
     * @param duration the duration defining the time between each instance of this recurring job
     * @param job      the lambda which defines the fire-and-forget job
     * @return the id of this recurring job which can be used to alter or delete it
     */
    public static String scheduleRecurrently(Duration duration, JobLambda job) {
        verifyJobScheduler();
        return jobScheduler.scheduleRecurrently(duration, job);
    }

    /**
     * Creates a new recurring job based on the given duration and the given lambda. The IoC container will be used to resolve {@code MyService}. The first run of this recurring job will happen after the given duration unless your duration is smaller or equal than your backgroundJobServer pollInterval.
     * <h5>An example:</h5>
     * <pre>{@code
     *      BackgroundJob.<MyService>scheduleRecurrently(Duration.parse("P5D"), x -> x.doWork());
     * }</pre>
     *
     * @param duration the duration defining the time between each instance of this recurring job
     * @param iocJob   the lambda which defines the fire-and-forget job
     * @return the id of this recurring job which can be used to alter or delete it
     */
    public static <S> String scheduleRecurrently(Duration duration, IocJobLambda<S> iocJob) {
        verifyJobScheduler();
        return jobScheduler.scheduleRecurrently(duration, iocJob);
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
     * @param job      the lambda which defines the fire-and-forget job
     * @return the id of this recurring job which can be used to alter or delete it
     */
    public static String scheduleRecurrently(String id, Duration duration, JobLambda job) {
        verifyJobScheduler();
        return jobScheduler.scheduleRecurrently(id, duration, job);
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
     * @param iocJob   the lambda which defines the fire-and-forget job
     * @return the id of this recurring job which can be used to alter or delete it
     */
    public static <S> String scheduleRecurrently(String id, Duration duration, IocJobLambda<S> iocJob) {
        verifyJobScheduler();
        return jobScheduler.scheduleRecurrently(id, duration, iocJob);
    }

    /**
     * Creates a new or alters the existing recurring job based on the given {@link RecurringJobBuilder}.
     * <h5>An example:</h5>
     * <pre>{@code
     *
     *      BackgroundJob.createRecurrently(aRecurringJob()
     *                                        .withCron("* * 0 * * *")
     *                                        .withDetails(() -> service.sendMail(toRequestParam, subjectRequestParam, bodyRequestParam));
     * }</pre>
     *
     * @param recurringJobBuilder the builder defining the recurring job
     * @return the id of this recurring job which can be used to alter or delete it
     */
    public static String createRecurrently(RecurringJobBuilder recurringJobBuilder) {
        verifyJobScheduler();
        return jobScheduler.createRecurrently(recurringJobBuilder);
    }

    /**
     * Deletes the recurring job based on the given id.
     * <h5>An example:</h5>
     * <pre>{@code
     *      BackgroundJob.delete("my-recurring-job"));
     * }</pre>
     *
     * @param id the id of the recurring job to delete
     */
    public static void delete(String id) {
        verifyJobScheduler();
        jobScheduler.delete(id);
    }

    private static void verifyJobScheduler() {
        if (jobScheduler != null) return;
        throw new IllegalStateException("The JobScheduler has not been initialized. Use the fluent JobRunr.configure() API to setup JobRunr or set the JobScheduler via the static setter method.");
    }

    public static void setJobScheduler(JobScheduler jobScheduler) {
        BackgroundJob.jobScheduler = jobScheduler;
    }
}
