package org.jobrunr.scheduling;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.details.JobDetailsAsmGenerator;
import org.jobrunr.jobs.details.JobDetailsGenerator;
import org.jobrunr.jobs.filters.JobFilter;
import org.jobrunr.jobs.filters.JobFilters;
import org.jobrunr.jobs.lambdas.IocJobLambda;
import org.jobrunr.jobs.lambdas.IocJobLambdaFromStream;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.jobs.lambdas.JobLambdaFromStream;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.scheduling.cron.CronExpression;
import org.jobrunr.storage.StorageProvider;

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

    private final StorageProvider storageProvider;
    private final JobDetailsGenerator jobDetailsGenerator;
    private final JobFilters jobFilters;
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
        this.jobFilters = new JobFilters(jobFilters);
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
        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(job);
        return enqueue(jobDetails);
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
        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(iocJob);
        return enqueue(jobDetails);
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
     *      jobScheduler.schedule(() -> service.doWork(), ZonedDateTime.now().plusHours(5));
     * }</pre>
     *
     * @param job           the lambda which defines the fire-and-forget job
     * @param zonedDateTime The moment in time at which the job will be enqueued.
     * @return the id of the Job
     */
    public JobId schedule(JobLambda job, ZonedDateTime zonedDateTime) {
        return schedule(job, zonedDateTime.toInstant());
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time. The IoC container will be used to resolve {@code MyService}.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.<MyService>schedule(x -> x.doWork(), ZonedDateTime.now().plusHours(5));
     * }</pre>
     *
     * @param iocJob        the lambda which defines the fire-and-forget job
     * @param zonedDateTime The moment in time at which the job will be enqueued.
     * @return the id of the Job
     */
    public <S> JobId schedule(IocJobLambda<S> iocJob, ZonedDateTime zonedDateTime) {
        return schedule(iocJob, zonedDateTime.toInstant());
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time.
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      jobScheduler.schedule(() -> service.doWork(), OffsetDateTime.now().plusHours(5));
     * }</pre>
     *
     * @param job            the lambda which defines the fire-and-forget job
     * @param offsetDateTime The moment in time at which the job will be enqueued.
     * @return the id of the Job
     */
    public JobId schedule(JobLambda job, OffsetDateTime offsetDateTime) {
        return schedule(job, offsetDateTime.toInstant());
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time. The IoC container will be used to resolve {@code MyService}.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.<MyService>schedule(x -> x.doWork(), OffsetDateTime.now().plusHours(5));
     * }</pre>
     *
     * @param iocJob         the lambda which defines the fire-and-forget job
     * @param offsetDateTime The moment in time at which the job will be enqueued.
     * @return the id of the Job
     */
    public <S> JobId schedule(IocJobLambda<S> iocJob, OffsetDateTime offsetDateTime) {
        return schedule(iocJob, offsetDateTime.toInstant());
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time.
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      jobScheduler.schedule(() -> service.doWork(), LocalDateTime.now().plusHours(5));
     * }</pre>
     *
     * @param job           the lambda which defines the fire-and-forget job
     * @param localDateTime The moment in time at which the job will be enqueued. It will use the systemDefault ZoneId to transform it to an UTC Instant
     * @return the id of the Job
     */
    public JobId schedule(JobLambda job, LocalDateTime localDateTime) {
        return schedule(job, localDateTime.atZone(systemDefault()).toInstant());
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time. The IoC container will be used to resolve {@code MyService}.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.<MyService>schedule(x -> x.doWork(), LocalDateTime.now().plusHours(5));
     * }</pre>
     *
     * @param iocJob        the lambda which defines the fire-and-forget job
     * @param localDateTime The moment in time at which the job will be enqueued. It will use the systemDefault ZoneId to transform it to an UTC Instant
     * @return the id of the Job
     */
    public <S> JobId schedule(IocJobLambda<S> iocJob, LocalDateTime localDateTime) {
        return schedule(iocJob, localDateTime.atZone(systemDefault()).toInstant());
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time.
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      jobScheduler.schedule(() -> service.doWork(), Instant.now().plusHours(5));
     * }</pre>
     *
     * @param job     the lambda which defines the fire-and-forget job
     * @param instant The moment in time at which the job will be enqueued.
     * @return the id of the Job
     */
    public JobId schedule(JobLambda job, Instant instant) {
        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(job);
        return schedule(jobDetails, instant);
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time. The IoC container will be used to resolve {@code MyService}.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.<MyService>schedule(x -> x.doWork(), Instant.now().plusHours(5));
     * }</pre>
     *
     * @param iocJob  the lambda which defines the fire-and-forget job
     * @param instant The moment in time at which the job will be enqueued.
     * @return the id of the Job
     */
    public <S> JobId schedule(IocJobLambda<S> iocJob, Instant instant) {
        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(iocJob);
        return schedule(jobDetails, instant);
    }

    /**
     * Deletes a job and sets it's state to DELETED. If the job is being processed, it will be interrupted.
     *
     * @param id the id of the job
     */
    public void delete(UUID id) {
        storageProvider.delete(id);
    }

    /**
     * @see #delete(UUID)
     */
    public void delete(JobId jobId) {
        storageProvider.delete(jobId.asUUID());
    }

    /**
     * Creates a new recurring job based on the given lambda and the given cron expression. The jobs will be scheduled using the systemDefault timezone.
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      jobScheduler.scheduleRecurringly(() -> service.doWork(), Cron.daily());
     * }</pre>
     *
     * @param job  the lambda which defines the fire-and-forget job
     * @param cron The cron expression defining when to run this recurring job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    public String scheduleRecurringly(JobLambda job, String cron) {
        return scheduleRecurringly(null, job, cron);
    }

    /**
     * Creates a new recurring job based on the given lambda and the given cron expression. The IoC container will be used to resolve {@code MyService}. The jobs will be scheduled using the systemDefault timezone.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.<MyService>scheduleRecurringly(x -> x.doWork(), Cron.daily());
     * }</pre>
     *
     * @param iocJob the lambda which defines the fire-and-forget job
     * @param cron   The cron expression defining when to run this recurring job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    public <S> String scheduleRecurringly(IocJobLambda<S> iocJob, String cron) {
        return scheduleRecurringly(null, iocJob, cron);
    }

    /**
     * Creates a new recurring job based on the given id, lambda and cron expression. The jobs will be scheduled using the systemDefault timezone
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      jobScheduler.scheduleRecurringly("my-recurring-job", () -> service.doWork(), Cron.daily());
     * }</pre>
     *
     * @param id   the id of this recurring job which can be used to alter or delete it
     * @param job  the lambda which defines the fire-and-forget job
     * @param cron The cron expression defining when to run this recurring job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    public String scheduleRecurringly(String id, JobLambda job, String cron) {
        return scheduleRecurringly(id, job, cron, systemDefault());
    }

    /**
     * Creates a new recurring job based on the given id, lambda and cron expression. The IoC container will be used to resolve {@code MyService}. The jobs will be scheduled using the systemDefault timezone
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.<MyService>scheduleRecurringly("my-recurring-job", x -> x.doWork(), Cron.daily());
     * }</pre>
     *
     * @param id     the id of this recurring job which can be used to alter or delete it
     * @param iocJob the lambda which defines the fire-and-forget job
     * @param cron   The cron expression defining when to run this recurring job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    public <S> String scheduleRecurringly(String id, IocJobLambda<S> iocJob, String cron) {
        return scheduleRecurringly(id, iocJob, cron, systemDefault());
    }

    /**
     * Creates a new recurring job based on the given id, lambda, cron expression and {@code ZoneId}.
     * <h5>An example:</h5>
     * <pre>{@code
     *      MyService service = new MyService();
     *      jobScheduler.scheduleRecurringly("my-recurring-job", () -> service.doWork(), Cron.daily(), ZoneId.of("Europe/Brussels"));
     * }</pre>
     *
     * @param id     the id of this recurring job which can be used to alter or delete it
     * @param job    the lambda which defines the fire-and-forget job
     * @param cron   The cron expression defining when to run this recurring job
     * @param zoneId The zoneId (timezone) of when to run this recurring job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    public String scheduleRecurringly(String id, JobLambda job, String cron, ZoneId zoneId) {
        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(job);
        return scheduleRecurringly(id, jobDetails, CronExpression.create(cron), zoneId);
    }

    /**
     * Creates a new recurring job based on the given id, lambda, cron expression and {@code ZoneId}. The IoC container will be used to resolve {@code MyService}.
     * <h5>An example:</h5>
     * <pre>{@code
     *      jobScheduler.<MyService>scheduleRecurringly("my-recurring-job", x -> x.doWork(), Cron.daily(), ZoneId.of("Europe/Brussels"));
     * }</pre>
     *
     * @param id     the id of this recurring job which can be used to alter or delete it
     * @param iocJob the lambda which defines the fire-and-forget job
     * @param cron   The cron expression defining when to run this recurring job
     * @param zoneId The zoneId (timezone) of when to run this recurring job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    public <S> String scheduleRecurringly(String id, IocJobLambda<S> iocJob, String cron, ZoneId zoneId) {
        JobDetails jobDetails = jobDetailsGenerator.toJobDetails(iocJob);
        return scheduleRecurringly(id, jobDetails, CronExpression.create(cron), zoneId);
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

    JobId enqueue(JobDetails jobDetails) {
        return saveJob(new Job(jobDetails));
    }

    JobId schedule(JobDetails jobDetails, Instant scheduleAt) {
        return saveJob(new Job(jobDetails, new ScheduledState(scheduleAt)));
    }

    JobId saveJob(Job job) {
        jobFilters.runOnCreatingFilter(job);
        Job savedJob = this.storageProvider.save(job);
        jobFilters.runOnCreatedFilter(savedJob);
        return new JobId(savedJob.getId());
    }

    List<Job> saveJobs(List<Job> jobs) {
        jobFilters.runOnCreatingFilter(jobs);
        final List<Job> savedJobs = this.storageProvider.save(jobs);
        jobFilters.runOnCreatedFilter(savedJobs);
        return savedJobs;
    }

    String scheduleRecurringly(String id, JobDetails jobDetails, CronExpression cronExpression, ZoneId zoneId) {
        final RecurringJob recurringJob = new RecurringJob(id, jobDetails, cronExpression, zoneId);
        jobFilters.runOnCreatingFilter(recurringJob);
        RecurringJob savedRecurringJob = this.storageProvider.saveRecurringJob(recurringJob);
        jobFilters.runOnCreatedFilter(recurringJob);
        return savedRecurringJob.getId();
    }
}
