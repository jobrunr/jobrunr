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
 *
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
     *
     * By default, this method is automatically called on construction of the StorageProvider
     *
     * @param databaseOptions defines whether to set up the StorageProvider or validate whether the StorageProvider is set up correctly.
     */
    void setUpStorageProvider(DatabaseOptions databaseOptions);

    void addJobStorageOnChangeListener(StorageProviderChangeListener listener);

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

    Job save(Job job);

    int deletePermanently(UUID id);

    Job getJobById(UUID id);

    List<Job> save(List<Job> jobs);

    List<Job> getJobs(StateName state, Instant updatedBefore, PageRequest pageRequest);

    List<Job> getScheduledJobs(Instant scheduledBefore, PageRequest pageRequest);

    List<Job> getJobs(StateName state, PageRequest pageRequest);

    Page<Job> getJobPage(StateName state, PageRequest pageRequest);

    int deleteJobsPermanently(StateName state, Instant updatedBefore);

    Set<String> getDistinctJobSignatures(StateName... states);

    boolean exists(JobDetails jobDetails, StateName... states);

    boolean recurringJobExists(String recurringJobId, StateName... states);

    RecurringJob saveRecurringJob(RecurringJob recurringJob);

    @Deprecated
    long countRecurringJobs();

    RecurringJobsResult getRecurringJobs();

    boolean recurringJobsUpdated(Long recurringJobsUpdatedHash);

    int deleteRecurringJob(String id);

    JobStats getJobStats();

    void publishTotalAmountOfSucceededJobs(int amount);

    default Job getJobById(JobId jobId) {
        return getJobById(jobId.asUUID());
    }

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
