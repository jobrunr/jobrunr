package org.jobrunr.scheduling;

import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.scheduling.carbonaware.CarbonAwarePeriod;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.temporal.Temporal;
import java.util.UUID;
import java.util.stream.Stream;

import static java.time.ZoneId.systemDefault;

/**
 * Provides static methods for creating fire-and-forget, delayed and recurring jobs as well as to delete existing background jobs.
 * If you prefer not to use a static accessor, you can inject the {@link JobRequestScheduler} which exposes the same methods.
 *
 * @author Ronald Dehuysser
 */
public class BackgroundJobRequest {

    private BackgroundJobRequest() {
    }

    private static JobRequestScheduler jobRequestScheduler;

    /**
     * Creates a new {@link org.jobrunr.jobs.Job} using a {@link JobBuilder} that can be enqueued or scheduled and provides an alternative to the job annotation.
     *
     * @param jobBuilder the {@link JobBuilder} with all the details of the job
     * @return the id of the job
     */
    public static JobId create(JobBuilder jobBuilder) {
        return jobRequestScheduler.create(jobBuilder);
    }

    /**
     * Creates a new {@link org.jobrunr.jobs.Job} for each {@link JobBuilder} and provides an alternative to the job annotation.
     *
     * @param jobBuilderStream the jobBuilders for which to create jobs.
     */
    public static void create(Stream<JobBuilder> jobBuilderStream) {
        jobRequestScheduler.create(jobBuilderStream);
    }

    /**
     * Creates a new fire-and-forget job based on a given {@link JobRequest}. JobRunr will try to find the JobRequestHandler in
     * the IoC container or else it will try to create the handler by calling the default no-arg constructor.
     * <h5>An example:</h5>
     * <pre>{@code
     *            BackgroundJobRequest.enqueue(new MyJobRequest());
     *       }</pre>
     *
     * @param jobRequest the {@link JobRequest} which defines the fire-and-forget job.
     * @return the id of the job
     */
    public static JobId enqueue(JobRequest jobRequest) {
        verifyJobScheduler();
        return jobRequestScheduler.enqueue(jobRequest);
    }

    /**
     * Creates a new fire-and-forget job based on a given {@link JobRequest}. JobRunr will try to find the JobRequestHandler in
     * the IoC container or else it will try to create the handler by calling the default no-arg constructor.
     * <h5>An example:</h5>
     * <pre>{@code
     *            BackgroundJobRequest.enqueueJobRequest(id, new MyJobRequest());
     *       }</pre>
     *
     * @param id         the uuid with which to save the job
     * @param jobRequest the {@link JobRequest} which defines the fire-and-forget job.
     * @return the id of the job
     */
    public static JobId enqueue(UUID id, JobRequest jobRequest) {
        verifyJobScheduler();
        return jobRequestScheduler.enqueue(id, jobRequest);
    }

    /**
     * Creates new fire-and-forget jobs for each item in the input stream. JobRunr will try to find the JobRequestHandler in
     * the IoC container or else it will try to create the handler by calling the default no-arg constructor.
     * <h5>An example:</h5>
     * <pre>{@code
     *      Stream<MyJobRequest> workStream = getWorkStream();
     *      BackgroundJobRequest.enqueue(workStream);
     * }</pre>
     *
     * @param input the stream of jobRequests for which to create fire-and-forget jobs
     */
    public static void enqueue(Stream<? extends JobRequest> input) {
        verifyJobScheduler();
        jobRequestScheduler.enqueue(input);
    }

    /**
     * Creates a new fire-and-forget job based on the given {@link JobRequest} and schedules it to be enqueued at the given moment of time. JobRunr will try to find the JobRequestHandler in
     * the IoC container or else it will try to create the handler by calling the default no-arg constructor.
     *
     * <h5>Supported {@link Temporal} implementations:</h5>
     * <ul>
     *     <li>{@link CarbonAwarePeriod} to schedule a Carbon Aware job</li>
     *     <li>{@link Instant}</li>
     *     <li>{@link ChronoLocalDateTime} (e.g., {@link LocalDateTime}): converted to {@link Instant} using {@link ZoneId#systemDefault()}</li>
     *     <li>{@link ChronoZonedDateTime} (e.g., {@link ZonedDateTime})</li>
     *     <li>{@link OffsetDateTime}</li>
     * </ul>
     *
     * <h5>An Example with {@code Instant}:</h5>
     * <pre>{@code
     *      BackgroundJobRequest.schedule(Instant.now().plus(5, ChronoUnit.HOURS), new MyJobRequest());
     * }</pre>
     *
     * <h5>An Example with {@code CarbonAwarePeriod}:</h5>
     * <pre>{@code
     *      BackgroundJobRequest.schedule(CarbonAware.between(Instant.now(), Instant.now().plus(5, ChronoUnit.HOURS)), new MyJobRequest());
     * }</pre>
     *
     * @param scheduleAt the moment in time at which the job will be enqueued.
     * @param jobRequest the {@link JobRequest} which defines the fire-and-forget job
     * @return the id of the Job
     */
    public static JobId schedule(Temporal scheduleAt, JobRequest jobRequest) {
        verifyJobScheduler();
        return jobRequestScheduler.schedule(scheduleAt, jobRequest);
    }

    /**
     * Creates a new fire-and-forget job based on the given {@link JobRequest} and schedules it to be enqueued at the given moment of time. JobRunr will try to find the JobRequestHandler in
     * the IoC container or else it will try to create the handler by calling the default no-arg constructor.
     * If a job with that id already exists, JobRunr will not save it again.
     *
     * <h5>Supported {@link Temporal} implementations:</h5>
     * <ul>
     *     <li>{@link CarbonAwarePeriod} to schedule a Carbon Aware job</li>
     *     <li>{@link Instant}</li>
     *     <li>{@link ChronoLocalDateTime} (e.g., {@link LocalDateTime}): converted to {@link Instant} using {@link ZoneId#systemDefault()}</li>
     *     <li>{@link ChronoZonedDateTime}) (e.g., {@link ZonedDateTime})</li>
     *     <li>{@link OffsetDateTime}</li>
     * </ul>
     *
     * <h5>An Example with {@code Instant}:</h5>
     * <pre>{@code
     *      BackgroundJobRequest.schedule(id, Instant.now().plus(5, ChronoUnit.HOURS), new MyJobRequest());
     * }</pre>
     *
     * <h5>An Example with {@code CarbonAwarePeriod}:</h5>
     * <pre>{@code
     *      BackgroundJobRequest.schedule(id, CarbonAware.between(Instant.now(), Instant.now().plus(5, ChronoUnit.HOURS)), new MyJobRequest());
     * }</pre>
     *
     * @param id         the uuid with which to save the job
     * @param scheduleAt the moment in time at which the job will be enqueued.
     * @param jobRequest the {@link JobRequest} which defines the fire-and-forget job
     * @return the id of the Job
     */
    public static JobId schedule(UUID id, Temporal scheduleAt, JobRequest jobRequest) {
        verifyJobScheduler();
        return jobRequestScheduler.schedule(id, scheduleAt, jobRequest);
    }

    /**
     * Deletes a job and sets its state to DELETED. If the job is being processed, it will be interrupted.
     *
     * @param id the id of the job
     */
    public static void delete(UUID id) {
        verifyJobScheduler();
        jobRequestScheduler.delete(id);
    }

    /**
     * @see #delete(UUID)
     */
    public static void delete(JobId jobId) {
        delete(jobId.asUUID());
    }

    /**
     * Creates a new recurring job based on the given cron expression and the given {@link JobRequest}. JobRunr will try to find the JobRequestHandler in
     * the IoC container or else it will try to create the handler by calling the default no-arg constructor. The jobs will be scheduled using the systemDefault timezone.
     * <h5>An example:</h5>
     * <pre>{@code
     *      BackgroundJobRequest.scheduleRecurrently(Cron.daily(), new MyJobRequest());
     * }</pre>
     *
     * @param cron       The cron expression defining when to run this recurring job
     * @param jobRequest the {@link JobRequest} which defines the recurring job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    public static String scheduleRecurrently(String cron, JobRequest jobRequest) {
        verifyJobScheduler();
        return jobRequestScheduler.scheduleRecurrently(cron, jobRequest);
    }

    /**
     * Creates a new or alters the existing recurring job based on the given id, cron expression and {@link JobRequest}. JobRunr will try to find the JobRequestHandler in
     * the IoC container or else it will try to create the handler by calling the default no-arg constructor. The jobs will be scheduled using the systemDefault timezone
     * <h5>An example:</h5>
     * <pre>{@code
     *      BackgroundJobRequest.scheduleRecurrently("my-recurring-job", Cron.daily(), new MyJobRequest());
     * }</pre>
     *
     * @param id         the id of this recurring job which can be used to alter or delete it
     * @param cron       The cron expression defining when to run this recurring job
     * @param jobRequest the jobRequest which defines the recurring job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    public static String scheduleRecurrently(String id, String cron, JobRequest jobRequest) {
        verifyJobScheduler();
        return jobRequestScheduler.scheduleRecurrently(id, cron, systemDefault(), jobRequest);
    }

    /**
     * Creates a new or alters the existing recurring job based on the given id, cron expression, {@link  ZoneId} and {@link JobRequest}. JobRunr will try to find the JobRequestHandler in
     * the IoC container or else it will try to create the handler by calling the default no-arg constructor.
     * <h5>An example:</h5>
     * <pre>{@code
     *      BackgroundJobRequest.scheduleRecurrently("my-recurring-job", Cron.daily(), ZoneId.of("Europe/Brussels"), new MyJobRequest());
     * }</pre>
     *
     * @param id         the id of this recurring job which can be used to alter or delete it
     * @param cron       The cron expression defining when to run this recurring job
     * @param zoneId     The zoneId (timezone) of when to run this recurring job
     * @param jobRequest the {@link JobRequest} which defines the recurring job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    public static String scheduleRecurrently(String id, String cron, ZoneId zoneId, JobRequest jobRequest) {
        verifyJobScheduler();
        return jobRequestScheduler.scheduleRecurrently(id, cron, zoneId, jobRequest);
    }

    /**
     * Creates a new recurring job based on the given duration and the given {@link JobRequest}. The first run of this recurring job will happen after the given duration unless your duration is smaller or equal than your backgroundJobServer pollInterval.
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      BackgroundJob.scheduleRecurrently(Duration.parse("P5D"), () -> service.doWork());
     * }</pre>
     *
     * @param duration   the duration defining the time between each instance of this recurring job
     * @param jobRequest the jobRequest which defines the recurring job
     * @return the id of this recurring job which can be used to alter or delete it
     */
    public static String scheduleRecurrently(Duration duration, JobRequest jobRequest) {
        verifyJobScheduler();
        return jobRequestScheduler.scheduleRecurrently(duration, jobRequest);
    }

    /**
     * Creates a new or alters the existing recurring job based on the given id, duration and {@link JobRequest}. The first run of this recurring job will happen after the given duration unless your duration is smaller or equal than your backgroundJobServer pollInterval.
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      BackgroundJob.scheduleRecurrently("my-recurring-job", Duration.parse("P5D"), () -> service.doWork());
     * }</pre>
     *
     * @param id         the id of this recurring job which can be used to alter or delete it
     * @param duration   the duration defining the time between each instance of this recurring job
     * @param jobRequest the {@link JobRequest} which defines the recurring job
     * @return the id of this recurring job which can be used to alter or delete it
     */
    public static String scheduleRecurrently(String id, Duration duration, JobRequest jobRequest) {
        verifyJobScheduler();
        return jobRequestScheduler.scheduleRecurrently(id, duration, jobRequest);
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
        return jobRequestScheduler.createRecurrently(recurringJobBuilder);
    }

    /**
     * Deletes the recurring job based on the given id.
     * <h5>An example:</h5>
     * <pre>{@code
     *      BackgroundJobRequest.deleteRecurringJob("my-recurring-job"));
     * }</pre>
     *
     * @param id the id of the recurring job to delete
     */
    public static void deleteRecurringJob(String id) {
        verifyJobScheduler();
        jobRequestScheduler.deleteRecurringJob(id);
    }

    private static void verifyJobScheduler() {
        if (jobRequestScheduler != null) return;
        throw new IllegalStateException("The JobRequestScheduler has not been initialized. Use the fluent JobRunr.configure() API to setup JobRunr or set the JobRequestScheduler via the static setter method.");
    }

    public static void setJobRequestScheduler(JobRequestScheduler jobRequestScheduler) {
        BackgroundJobRequest.jobRequestScheduler = jobRequestScheduler;
    }
}
