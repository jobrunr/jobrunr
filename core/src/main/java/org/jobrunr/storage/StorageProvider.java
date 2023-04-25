package org.jobrunr.storage;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.storage.listeners.StorageProviderChangeListener;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The StorageProvider allows to store, retrieve and delete background jobs.
 * <p>
 * This API is public and JobRunr major version will change of this StorageProvider API changes.
 *
 * @since 0.9.0
 */
public interface StorageProvider extends AutoCloseable {

    int BATCH_SIZE = 5000;

    StorageProviderInfo getStorageProviderInfo();

    void setJobMapper(JobMapper jobMapper);

    /**
     * This method allows to reinitialize the StorageProvider.
     * It can be used if you are using Flyway or Liquibase to setup your database manually.
     * <p>
     * By default, this method is automatically called on construction of the StorageProvider
     *
     * @param databaseOptions defines whether to set up the StorageProvider or validate whether the StorageProvider is set up correctly.
     */
    void setUpStorageProvider(DatabaseOptions databaseOptions);

    /**
     * Allows to listen for changes related to {@link Job jobs}.
     *
     * @param listener the listener to notify if there are any changes.
     * @see org.jobrunr.storage.listeners.JobChangeListener
     * @see org.jobrunr.storage.listeners.JobStatsChangeListener
     */
    void addJobStorageOnChangeListener(StorageProviderChangeListener listener);

    /**
     * Remove the given listener that listens for changes to {@link Job Jobs}
     *
     * @param listener the listener to remove
     */
    void removeJobStorageOnChangeListener(StorageProviderChangeListener listener);

    void announceBackgroundJobServer(BackgroundJobServerStatus serverStatus);

    boolean signalBackgroundJobServerAlive(BackgroundJobServerStatus serverStatus);

    void signalBackgroundJobServerStopped(BackgroundJobServerStatus serverStatus);

    List<BackgroundJobServerStatus> getBackgroundJobServers();

    UUID getLongestRunningBackgroundJobServerId();

    int removeTimedOutBackgroundJobServers(Instant heartbeatOlderThan);

    void saveMetadata(JobRunrMetadata metadata);

    List<JobRunrMetadata> getMetadata(String name);

    JobRunrMetadata getMetadata(String name, String owner);

    void deleteMetadata(String name);

    /**
     * Save the {@link Job} and increases the version if saving succeeded.
     *
     * @param job the job to save
     * @return the same job with an increased version
     * @throws ConcurrentJobModificationException if the already stored job was newer then the given version
     */
    Job save(Job job) throws ConcurrentJobModificationException;

    /**
     * Saves a list of {@link Job Jobs} and increases the version of each successfully saved {@link Job}.
     *
     * @param jobs the list of jobs to save
     * @return the same list of jobs with an increased version
     * @throws ConcurrentJobModificationException if any already stored job was newer then the given version
     */
    List<Job> save(List<Job> jobs) throws ConcurrentJobModificationException;

    /**
     * Returns the {@link Job} with the given id or throws a {@link JobNotFoundException} if the job does not exist
     *
     * @param id the id of the Job to fetch
     * @return the requested Job
     * @throws JobNotFoundException if the job is not found or does not exist anymore.
     */
    Job getJobById(UUID id) throws JobNotFoundException;

    /**
     * Returns the {@link Job} with the given id or throws a {@link JobNotFoundException} if the job does not exist
     *
     * @param jobId the id of the Job to fetch
     * @return the requested Job
     * @throws JobNotFoundException if the job is not found or does not exist anymore.
     */
    default Job getJobById(JobId jobId) throws JobNotFoundException {
        return getJobById(jobId.asUUID());
    }

    List<Job> getJobs(StateName state, Instant updatedBefore, PageRequest pageRequest);

    List<Job> getScheduledJobs(Instant scheduledBefore, PageRequest pageRequest);

    List<Job> getJobs(StateName state, PageRequest pageRequest);

    Page<Job> getJobPage(StateName state, PageRequest pageRequest);

    /**
     * Deletes the {@link Job} with the given id and returns the amount of deleted jobs (either 0 or 1).
     *
     * @param id the id of the Job to delete
     * @return 1 if the job with the given id was deleted, 0 otherwise
     */
    int deletePermanently(UUID id);

    int deleteJobsPermanently(StateName state, Instant updatedBefore);

    Set<String> getDistinctJobSignatures(StateName... states);

    boolean exists(JobDetails jobDetails, StateName... states);

    /**
     * Returns true when a {@link Job} created by the {@link RecurringJob} with the given id exists with one of the given states.
     *
     * @param recurringJobId the id of the RecurringJob for which the check whether a Job exists
     * @param states         the possible states for the Job (can be empty)
     * @return true if a Job exists created by a RecurringJob with the given id.
     */
    boolean recurringJobExists(String recurringJobId, StateName... states);

    /**
     * Saves a {@link RecurringJob} to the database. If a {@link RecurringJob} with the same id exists, it will be overwritten
     *
     * @param recurringJob the RecurringJob to save
     * @return the same RecurringJob
     */
    RecurringJob saveRecurringJob(RecurringJob recurringJob);

    @Deprecated
    long countRecurringJobs();

    /**
     * Returns a list {@link RecurringJob RecurringJobs}.
     *
     * @return a list {@link RecurringJob RecurringJobs}.
     */
    RecurringJobsResult getRecurringJobs();

    boolean recurringJobsUpdated(Long recurringJobsUpdatedHash);

    /**
     * Deletes the {@link RecurringJob} with the given id.
     *
     * @param id the id of the RecurringJob to delete
     * @return 1 if the RecurringJob with the given id was deleted, 0 otherwise
     */
    int deleteRecurringJob(String id);

    /**
     * Returns the statistics of the jobs (amount enqueued, amount scheduled, ...)
     * <em><strong>Important</strong>: in most cases, this results in a intensive query. JobRunr is designed to not call this method too often to limit
     * database CPU utilization. If you need access to this info, please use a {@link org.jobrunr.storage.listeners.JobStatsChangeListener} and register
     * it using the {@link StorageProvider#addJobStorageOnChangeListener(StorageProviderChangeListener)}.</em>
     *
     * @return the {@link JobStats}
     */
    JobStats getJobStats();

    void publishTotalAmountOfSucceededJobs(int amount);

    void close();

    class StorageProviderInfo {

        private final StorageProvider storageProvider;

        protected StorageProviderInfo(StorageProvider storageProvider) {
            this.storageProvider = storageProvider;
        }

        public String getName() {
            return storageProvider.getClass().getSimpleName();
        }

        public Class<? extends StorageProvider> getImplementationClass() {
            return storageProvider.getClass();
        }
    }
}
