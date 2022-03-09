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
 */
public interface StorageProvider extends AutoCloseable {

    String getName();

    void addJobStorageOnChangeListener(StorageProviderChangeListener listener);

    void removeJobStorageOnChangeListener(StorageProviderChangeListener listener);

    void setJobMapper(JobMapper jobMapper);

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

    List<RecurringJob> getRecurringJobs();

    long countRecurringJobs();

    int deleteRecurringJob(String id);

    JobStats getJobStats();

    void publishTotalAmountOfSucceededJobs(int amount);

    default Job getJobById(JobId jobId) {
        return getJobById(jobId.asUUID());
    }

    void close();

    void setUpStorageProvider(DatabaseOptions databaseOptions);
}
