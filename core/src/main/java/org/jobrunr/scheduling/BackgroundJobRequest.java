package org.jobrunr.scheduling;

import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.lambdas.JobRequest;

import java.time.*;
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
     * Creates a new fire-and-forget job based on a given jobRequest. JobRunr will try to find the JobRequestHandler in
     * the IoC container or else it will try to create the handler by calling the default no-arg constructor.
     * <h5>An example:</h5>
     * <pre>{@code
     *            BackgroundJobRequest.enqueue(new MyJobRequest());
     *       }</pre>
     *
     * @param jobRequest the jobRequest which defines the fire-and-forget job.
     * @return the id of the job
     */
    public static JobId enqueue(JobRequest jobRequest) {
        verifyJobScheduler();
        return jobRequestScheduler.enqueue(jobRequest);
    }

    /**
     * Creates a new fire-and-forget job based on a given jobRequest. JobRunr will try to find the JobRequestHandler in
     * the IoC container or else it will try to create the handler by calling the default no-arg constructor.
     * <h5>An example:</h5>
     * <pre>{@code
     *            BackgroundJobRequest.enqueueJobRequest(id, new MyJobRequest());
     *       }</pre>
     *
     * @param id         the uuid with which to save the job
     * @param jobRequest the jobRequest which defines the fire-and-forget job.
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
     * Creates a new fire-and-forget job based on the given jobRequest and schedules it to be enqueued at the given moment of time. JobRunr will try to find the JobRequestHandler in
     * the IoC container or else it will try to create the handler by calling the default no-arg constructor.
     * <h5>An example:</h5>
     * <pre>{@code
     *      BackgroundJobRequest.schedule(ZonedDateTime.now().plusHours(5), new MyJobRequest());
     * }</pre>
     *
     * @param zonedDateTime the moment in time at which the job will be enqueued.
     * @param jobRequest    the jobRequest which defines the fire-and-forget job
     * @return the id of the Job
     */
    public static JobId schedule(ZonedDateTime zonedDateTime, JobRequest jobRequest) {
        verifyJobScheduler();
        return jobRequestScheduler.schedule(zonedDateTime.toInstant(), jobRequest);
    }

    /**
     * Creates a new fire-and-forget job based on the given jobRequest and schedules it to be enqueued at the given moment of time. JobRunr will try to find the JobRequestHandler in
     * the IoC container or else it will try to create the handler by calling the default no-arg constructor.
     * If a job with that id already exists, JobRunr will not save it again.
     * <h5>An example:</h5>
     * <pre>{@code
     *      BackgroundJobRequest.schedule(id, ZonedDateTime.now().plusHours(5), new MyJobRequest());
     * }</pre>
     *
     * @param id            the uuid with which to save the job
     * @param zonedDateTime the moment in time at which the job will be enqueued.
     * @param jobRequest    the jobRequest which defines the fire-and-forget job
     * @return the id of the Job
     */
    public static JobId schedule(UUID id, ZonedDateTime zonedDateTime, JobRequest jobRequest) {
        verifyJobScheduler();
        return jobRequestScheduler.schedule(id, zonedDateTime.toInstant(), jobRequest);
    }

    /**
     * Creates a new fire-and-forget job based on the given jobRequest and schedules it to be enqueued at the given moment of time. JobRunr will try to find the JobRequestHandler in
     * the IoC container or else it will try to create the handler by calling the default no-arg constructor.
     * <h5>An example:</h5>
     * <pre>{@code
     *      BackgroundJobRequest.schedule(OffsetDateTime.now().plusHours(5), new MyJobRequest());
     * }</pre>
     *
     * @param offsetDateTime the moment in time at which the job will be enqueued.
     * @param jobRequest     the jobRequest which defines the fire-and-forget job
     * @return the id of the Job
     */
    public static JobId schedule(OffsetDateTime offsetDateTime, JobRequest jobRequest) {
        verifyJobScheduler();
        return jobRequestScheduler.schedule(offsetDateTime.toInstant(), jobRequest);
    }

    /**
     * Creates a new fire-and-forget job based on the given jobRequest and schedules it to be enqueued at the given moment of time. JobRunr will try to find the JobRequestHandler in
     * the IoC container or else it will try to create the handler by calling the default no-arg constructor.
     * If a job with that id already exists, JobRunr will not save it again.
     * <h5>An example:</h5>
     * <pre>{@code
     *      BackgroundJobRequest.schedule(id, OffsetDateTime.now().plusHours(5), new MyJobRequest());
     * }</pre>
     *
     * @param id             the uuid with which to save the job
     * @param offsetDateTime the moment in time at which the job will be enqueued.
     * @param jobRequest     the jobRequest which defines the fire-and-forget job
     * @return the id of the Job
     */
    public static JobId schedule(UUID id, OffsetDateTime offsetDateTime, JobRequest jobRequest) {
        verifyJobScheduler();
        return jobRequestScheduler.schedule(id, offsetDateTime.toInstant(), jobRequest);
    }

    /**
     * Creates a new fire-and-forget job based on the given jobRequest and schedules it to be enqueued at the given moment of time. JobRunr will try to find the JobRequestHandler in
     * the IoC container or else it will try to create the handler by calling the default no-arg constructor.
     * <h5>An example:</h5>
     * <pre>{@code
     *      BackgroundJobRequest.schedule(LocalDateTime.now().plusHours(5), new MyJobRequest());
     * }</pre>
     *
     * @param localDateTime the moment in time at which the job will be enqueued. It will use the systemDefault ZoneId to transform it to an UTC Instant
     * @param jobRequest    the jobRequest which defines the fire-and-forget job
     * @return the id of the Job
     */
    public static JobId schedule(LocalDateTime localDateTime, JobRequest jobRequest) {
        verifyJobScheduler();
        return jobRequestScheduler.schedule(localDateTime.atZone(systemDefault()).toInstant(), jobRequest);
    }

    /**
     * Creates a new fire-and-forget job based on the given jobRequest and schedules it to be enqueued at the given moment of time. JobRunr will try to find the JobRequestHandler in
     * the IoC container or else it will try to create the handler by calling the default no-arg constructor.
     * If a job with that id already exists, JobRunr will not save it again.
     * <h5>An example:</h5>
     * <pre>{@code
     *      BackgroundJobRequest.schedule(id, LocalDateTime.now().plusHours(5), new MyJobRequest());
     * }</pre>
     *
     * @param id            the uuid with which to save the job
     * @param localDateTime the moment in time at which the job will be enqueued. It will use the systemDefault ZoneId to transform it to an UTC Instant
     * @param jobRequest    the jobRequest which defines the fire-and-forget job
     * @return the id of the Job
     */
    public static JobId schedule(UUID id, LocalDateTime localDateTime, JobRequest jobRequest) {
        verifyJobScheduler();
        return jobRequestScheduler.schedule(id, localDateTime.atZone(systemDefault()).toInstant(), jobRequest);
    }

    /**
     * Creates a new fire-and-forget job based on the given jobRequest and schedules it to be enqueued at the given moment of time. JobRunr will try to find the JobRequestHandler in
     * the IoC container or else it will try to create the handler by calling the default no-arg constructor.
     * <h5>An example:</h5>
     * <pre>{@code
     *      BackgroundJobRequest.schedule(Instant.now().plusHours(5), new MyJobRequest());
     * }</pre>
     *
     * @param instant    the moment in time at which the job will be enqueued.
     * @param jobRequest the lambda which defines the fire-and-forget job
     * @return the id of the Job
     */
    public static JobId schedule(Instant instant, JobRequest jobRequest) {
        verifyJobScheduler();
        return jobRequestScheduler.schedule(instant, jobRequest);
    }

    /**
     * Creates a new fire-and-forget job based on the given jobRequest and schedules it to be enqueued at the given moment of time. JobRunr will try to find the JobRequestHandler in
     * the IoC container or else it will try to create the handler by calling the default no-arg constructor.
     * If a job with that id already exists, JobRunr will not save it again.
     * <h5>An example:</h5>
     * <pre>{@code
     *      BackgroundJobRequest.schedule(id, Instant.now().plusHours(5), new MyJobRequest());
     * }</pre>
     *
     * @param id         the uuid with which to save the job
     * @param instant    the moment in time at which the job will be enqueued.
     * @param jobRequest the jobRequest which defines the fire-and-forget job
     * @return the id of the Job
     */
    public static JobId schedule(UUID id, Instant instant, JobRequest jobRequest) {
        verifyJobScheduler();
        return jobRequestScheduler.schedule(id, instant, jobRequest);
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
     * Creates a new recurring job based on the given cron expression and the given jobRequest. JobRunr will try to find the JobRequestHandler in
     * the IoC container or else it will try to create the handler by calling the default no-arg constructor. The jobs will be scheduled using the systemDefault timezone.
     * <h5>An example:</h5>
     * <pre>{@code
     *      BackgroundJobRequest.scheduleRecurrently(Cron.daily(), new MyJobRequest());
     * }</pre>
     *
     * @param cron       The cron expression defining when to run this recurring job
     * @param jobRequest the jobRequest which defines the fire-and-forget job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    public static String scheduleRecurrently(String cron, JobRequest jobRequest) {
        verifyJobScheduler();
        return jobRequestScheduler.scheduleRecurrently(cron, jobRequest);
    }

    /**
     * Creates a new or alters the existing recurring job based on the given id, cron expression and jobRequest. JobRunr will try to find the JobRequestHandler in
     * the IoC container or else it will try to create the handler by calling the default no-arg constructor. The jobs will be scheduled using the systemDefault timezone
     * <h5>An example:</h5>
     * <pre>{@code
     *      BackgroundJobRequest.scheduleRecurrently("my-recurring-job", Cron.daily(), new MyJobRequest());
     * }</pre>
     *
     * @param id         the id of this recurring job which can be used to alter or delete it
     * @param cron       The cron expression defining when to run this recurring job
     * @param jobRequest the jobRequest which defines the fire-and-forget job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    public static String scheduleRecurrently(String id, String cron, JobRequest jobRequest) {
        verifyJobScheduler();
        return jobRequestScheduler.scheduleRecurrently(id, cron, systemDefault(), jobRequest);
    }

    /**
     * Creates a new or alters the existing recurring job based on the given id, cron expression, {@code ZoneId} and jobRequest. JobRunr will try to find the JobRequestHandler in
     * the IoC container or else it will try to create the handler by calling the default no-arg constructor.
     * <h5>An example:</h5>
     * <pre>{@code
     *      BackgroundJobRequest.scheduleRecurrently("my-recurring-job", Cron.daily(), ZoneId.of("Europe/Brussels"), new MyJobRequest());
     * }</pre>
     *
     * @param id         the id of this recurring job which can be used to alter or delete it
     * @param cron       The cron expression defining when to run this recurring job
     * @param zoneId     The zoneId (timezone) of when to run this recurring job
     * @param jobRequest the jobRequest which defines the fire-and-forget job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    public static String scheduleRecurrently(String id, String cron, ZoneId zoneId, JobRequest jobRequest) {
        verifyJobScheduler();
        return jobRequestScheduler.scheduleRecurrently(id, cron, zoneId, jobRequest);
    }

    /**
     * Deletes the recurring job based on the given id.
     * <h5>An example:</h5>
     * <pre>{@code
     *      BackgroundJobRequest.delete("my-recurring-job"));
     * }</pre>
     *
     * @param id the id of the recurring job to delete
     */
    public static void delete(String id) {
        verifyJobScheduler();
        jobRequestScheduler.delete(id);
    }

    private static void verifyJobScheduler() {
        if (jobRequestScheduler != null) return;
        throw new IllegalStateException("The JobRequestScheduler has not been initialized. Use the fluent JobRunr.configure() API to setup JobRunr or set the JobRequestScheduler via the static setter method.");
    }

    public static void setJobRequestScheduler(JobRequestScheduler jobRequestScheduler) {
        BackgroundJobRequest.jobRequestScheduler = jobRequestScheduler;
    }
}
