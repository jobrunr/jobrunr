package org.jobrunr.scheduling;

import kotlin.Function;
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

    /** TODO */
    public static JobId enqueue(Function<?> job) {
        verifyJobScheduler();
        return jobScheduler.enqueue(job);
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
     *      BackgroundJob.schedule(() -> service.doWork(), ZonedDateTime.now().plusHours(5));
     * }</pre>
     *
     * @param job           the lambda which defines the fire-and-forget job
     * @param zonedDateTime The moment in time at which the job will be enqueued.
     * @return the id of the Job
     */
    public static JobId schedule(JobLambda job, ZonedDateTime zonedDateTime) {
        verifyJobScheduler();
        return jobScheduler.schedule(job, zonedDateTime);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time. The IoC container will be used to resolve {@code MyService}.
     * <h5>An example:</h5>
     * <pre>{@code
     *      BackgroundJob.<MyService>schedule(x -> x.doWork(), ZonedDateTime.now().plusHours(5));
     * }</pre>
     *
     * @param iocJob        the lambda which defines the fire-and-forget job
     * @param zonedDateTime The moment in time at which the job will be enqueued.
     * @return the id of the Job
     */
    public static <S> JobId schedule(IocJobLambda<S> iocJob, ZonedDateTime zonedDateTime) {
        verifyJobScheduler();
        return jobScheduler.schedule(iocJob, zonedDateTime);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time.
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      BackgroundJob.schedule(() -> service.doWork(), OffsetDateTime.now().plusHours(5));
     * }</pre>
     *
     * @param job            the lambda which defines the fire-and-forget job
     * @param offsetDateTime The moment in time at which the job will be enqueued.
     * @return the id of the Job
     */
    public static JobId schedule(JobLambda job, OffsetDateTime offsetDateTime) {
        verifyJobScheduler();
        return jobScheduler.schedule(job, offsetDateTime);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time. The IoC container will be used to resolve {@code MyService}.
     * <h5>An example:</h5>
     * <pre>{@code
     *      BackgroundJob.<MyService>schedule(x -> x.doWork(), OffsetDateTime.now().plusHours(5));
     * }</pre>
     *
     * @param iocJob         the lambda which defines the fire-and-forget job
     * @param offsetDateTime The moment in time at which the job will be enqueued.
     * @return the id of the Job
     */
    public static <S> JobId schedule(IocJobLambda<S> iocJob, OffsetDateTime offsetDateTime) {
        verifyJobScheduler();
        return jobScheduler.schedule(iocJob, offsetDateTime);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time.
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      BackgroundJob.schedule(() -> service.doWork(), LocalDateTime.now().plusHours(5));
     * }</pre>
     *
     * @param job           the lambda which defines the fire-and-forget job
     * @param localDateTime The moment in time at which the job will be enqueued. It will use the systemDefault ZoneId to transform it to an UTC Instant
     * @return the id of the Job
     */
    public static JobId schedule(JobLambda job, LocalDateTime localDateTime) {
        verifyJobScheduler();
        return jobScheduler.schedule(job, localDateTime);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time. The IoC container will be used to resolve {@code MyService}.
     * <h5>An example:</h5>
     * <pre>{@code
     *      BackgroundJob.<MyService>schedule(x -> x.doWork(), LocalDateTime.now().plusHours(5));
     * }</pre>
     *
     * @param iocJob        the lambda which defines the fire-and-forget job
     * @param localDateTime The moment in time at which the job will be enqueued. It will use the systemDefault ZoneId to transform it to an UTC Instant
     * @return the id of the Job
     */
    public static <S> JobId schedule(IocJobLambda<S> iocJob, LocalDateTime localDateTime) {
        verifyJobScheduler();
        return jobScheduler.schedule(iocJob, localDateTime);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time.
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      BackgroundJob.schedule(() -> service.doWork(), Instant.now().plusHours(5));
     * }</pre>
     *
     * @param job     the lambda which defines the fire-and-forget job
     * @param instant The moment in time at which the job will be enqueued.
     * @return the id of the Job
     */
    public static JobId schedule(JobLambda job, Instant instant) {
        verifyJobScheduler();
        return jobScheduler.schedule(job, instant);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time. The IoC container will be used to resolve {@code MyService}.
     * <h5>An example:</h5>
     * <pre>{@code
     *      BackgroundJob.<MyService>schedule(x -> x.doWork(), Instant.now().plusHours(5));
     * }</pre>
     *
     * @param iocJob  the lambda which defines the fire-and-forget job
     * @param instant The moment in time at which the job will be enqueued.
     * @return the id of the Job
     */
    public static <S> JobId schedule(IocJobLambda<S> iocJob, Instant instant) {
        verifyJobScheduler();
        return jobScheduler.schedule(iocJob, instant);
    }

    /**
     * Deletes a job and sets it's state to DELETED. If the job is being processed, it will be interrupted.
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
     * Creates a new recurring job based on the given lambda and the given cron expression. The jobs will be scheduled using the systemDefault timezone.
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      BackgroundJob.scheduleRecurrently(() -> service.doWork(), Cron.daily());
     * }</pre>
     *
     * @param job  the lambda which defines the fire-and-forget job
     * @param cron The cron expression defining when to run this recurring job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    public static String scheduleRecurrently(JobLambda job, String cron) {
        verifyJobScheduler();
        return jobScheduler.scheduleRecurrently(job, cron);
    }

    /** TODO */
    public static String scheduleRecurrently(String cron, Function<?> job) {
        verifyJobScheduler();
        return jobScheduler.scheduleRecurrently(cron, job);
    }

    /**
     * Creates a new recurring job based on the given lambda and the given cron expression. The IoC container will be used to resolve {@code MyService}. The jobs will be scheduled using the systemDefault timezone.
     * <h5>An example:</h5>
     * <pre>{@code
     *      BackgroundJob.<MyService>scheduleRecurrently(x -> x.doWork(), Cron.daily());
     * }</pre>
     *
     * @param iocJob the lambda which defines the fire-and-forget job
     * @param cron   The cron expression defining when to run this recurring job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    public static <S> String scheduleRecurrently(IocJobLambda<S> iocJob, String cron) {
        verifyJobScheduler();
        return jobScheduler.scheduleRecurrently(iocJob, cron);
    }

    /**
     * Creates a new recurring job based on the given id, lambda and cron expression. The jobs will be scheduled using the systemDefault timezone
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      BackgroundJob.scheduleRecurrently("my-recurring-job", () -> service.doWork(), Cron.daily());
     * }</pre>
     *
     * @param id   the id of this recurring job which can be used to alter or delete it
     * @param job  the lambda which defines the fire-and-forget job
     * @param cron The cron expression defining when to run this recurring job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    public static String scheduleRecurrently(String id, JobLambda job, String cron) {
        verifyJobScheduler();
        return jobScheduler.scheduleRecurrently(id, job, cron);
    }

    /**
     * Creates a new recurring job based on the given id, lambda and cron expression. The IoC container will be used to resolve {@code MyService}. The jobs will be scheduled using the systemDefault timezone
     * <h5>An example:</h5>
     * <pre>{@code
     *      BackgroundJob.<MyService>scheduleRecurrently("my-recurring-job", x -> x.doWork(), Cron.daily());
     * }</pre>
     *
     * @param id     the id of this recurring job which can be used to alter or delete it
     * @param iocJob the lambda which defines the fire-and-forget job
     * @param cron   The cron expression defining when to run this recurring job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    public static <S> String scheduleRecurrently(String id, IocJobLambda<S> iocJob, String cron) {
        verifyJobScheduler();
        return jobScheduler.scheduleRecurrently(id, iocJob, cron);
    }

    /**
     * Creates a new recurring job based on the given id, lambda, cron expression and {@code ZoneId}.
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      BackgroundJob.scheduleRecurrently("my-recurring-job", () -> service.doWork(), Cron.daily(), ZoneId.of("Europe/Brussels"));
     * }</pre>
     *
     * @param id     the id of this recurring job which can be used to alter or delete it
     * @param job    the lambda which defines the fire-and-forget job
     * @param cron   The cron expression defining when to run this recurring job
     * @param zoneId The zoneId (timezone) of when to run this recurring job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    public static String scheduleRecurrently(String id, JobLambda job, String cron, ZoneId zoneId) {
        verifyJobScheduler();
        return jobScheduler.scheduleRecurrently(id, job, cron, zoneId);
    }

    /**
     * Creates a new recurring job based on the given id, lambda, cron expression and {@code ZoneId}. The IoC container will be used to resolve {@code MyService}.
     * <h5>An example:</h5>
     * <pre>{@code
     *      BackgroundJob.<MyService>scheduleRecurrently("my-recurring-job", x -> x.doWork(), Cron.daily(), ZoneId.of("Europe/Brussels"));
     * }</pre>
     *
     * @param id     the id of this recurring job which can be used to alter or delete it
     * @param iocJob the lambda which defines the fire-and-forget job
     * @param cron   The cron expression defining when to run this recurring job
     * @param zoneId The zoneId (timezone) of when to run this recurring job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    public static <S> String scheduleRecurrently(String id, IocJobLambda<S> iocJob, String cron, ZoneId zoneId) {
        verifyJobScheduler();
        return jobScheduler.scheduleRecurrently(id, iocJob, cron, zoneId);
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
