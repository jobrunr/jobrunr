package org.jobrunr.scheduling;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.filters.JobDefaultFilters;
import org.jobrunr.jobs.filters.JobFilter;
import org.jobrunr.jobs.filters.JobFilterUtils;
import org.jobrunr.jobs.mappers.MDCMapper;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.jobrunr.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public abstract class AbstractJobScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractJobScheduler.class);

    private final StorageProvider storageProvider;
    private final JobFilterUtils jobFilterUtils;

    /**
     * Creates a new AbstractJobScheduler using the provided storageProvider and the list of JobFilters that will be used for every background job
     *
     * @param storageProvider the storageProvider to use
     * @param jobFilters      list of jobFilters that will be used for every job
     */
    protected AbstractJobScheduler(StorageProvider storageProvider, List<JobFilter> jobFilters) {
        if (storageProvider == null)
            throw new IllegalArgumentException("A JobStorageProvider is required to use the JobScheduler. Please see the documentation on how to setup a JobStorageProvider.");
        this.storageProvider = storageProvider;
        this.jobFilterUtils = new JobFilterUtils(new JobDefaultFilters(jobFilters));
    }

    abstract JobId create(JobBuilder jobBuilder);

    abstract void create(Stream<JobBuilder> jobBuilderStream);

    /**
     * @see #delete(UUID)
     */
    public void delete(JobId jobId) {
        this.delete(jobId.asUUID());
    }

    /**
     * @see #delete(UUID, String)
     */
    public void delete(JobId jobId, String reason) {
        this.delete(jobId.asUUID(), reason);
    }

    /**
     * Deletes a job and sets its state to DELETED. If the job is being processed, it will be interrupted.
     *
     * @param id the id of the job
     */
    public void delete(UUID id) {
        delete(id, "Deleted via JobScheduler API");
    }

    /**
     * Deletes a job and sets its state to DELETED. If the job is being processed, it will be interrupted.
     *
     * @param id     the id of the job
     * @param reason the reason why the job is deleted.
     */
    public void delete(UUID id, String reason) {
        final Job jobToDelete = storageProvider.getJobById(id);
        jobToDelete.delete(reason);
        jobFilterUtils.runOnStateElectionFilter(jobToDelete);
        final Job deletedJob = storageProvider.save(jobToDelete);
        jobFilterUtils.runOnStateAppliedFilters(deletedJob);
        LOGGER.debug("Deleted Job with id {}", deletedJob.getId());
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

    abstract String createRecurrently(RecurringJobBuilder recurringJobBuilder);

    String scheduleRecurrently(String id, JobDetails jobDetails, Schedule schedule, ZoneId zoneId) {
        final RecurringJob recurringJob = new RecurringJob(id, jobDetails, schedule, zoneId);
        return scheduleRecurrently(recurringJob);
    }

    String scheduleRecurrently(RecurringJob recurringJob) {
        jobFilterUtils.runOnCreatingFilter(recurringJob);
        RecurringJob savedRecurringJob = this.storageProvider.saveRecurringJob(recurringJob);
        jobFilterUtils.runOnCreatedFilter(recurringJob);
        return savedRecurringJob.getId();
    }

    JobId saveJob(Job job) {
        try {
            MDCMapper.saveMDCContextToJob(job);
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
        jobs.forEach(MDCMapper::saveMDCContextToJob);
        jobFilterUtils.runOnCreatingFilter(jobs);
        final List<Job> savedJobs = this.storageProvider.save(jobs);
        jobFilterUtils.runOnCreatedFilter(savedJobs);
        return savedJobs;
    }
}
