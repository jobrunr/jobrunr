package org.jobrunr.storage;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.scheduling.JobId;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The StorageProvider allows to store, retrieve and delete background jobs.
 */
public interface StorageProvider extends AutoCloseable {

    void addJobStorageOnChangeListener(JobStorageChangeListener listener);

    void removeJobStorageOnChangeListener(JobStorageChangeListener listener);

    void setJobMapper(JobMapper jobMapper);

    void announceBackgroundJobServer(BackgroundJobServerStatus serverStatus);

    boolean signalBackgroundJobServerAlive(BackgroundJobServerStatus serverStatus);

    List<BackgroundJobServerStatus> getBackgroundJobServers();

    int removeTimedOutBackgroundJobServers(Instant heartbeatOlderThan);

    Job save(Job job);

    int delete(UUID id);

    Job getJobById(UUID id);

    List<Job> save(List<Job> jobs);

    List<Job> getJobs(StateName state, Instant updatedBefore, PageRequest pageRequest);

    List<Job> getScheduledJobs(Instant scheduledBefore, PageRequest pageRequest);

    Long countJobs(StateName state);

    List<Job> getJobs(StateName state, PageRequest pageRequest);

    Page<Job> getJobPage(StateName state, PageRequest pageRequest);

    int deleteJobs(StateName state, Instant updatedBefore);

    boolean exists(JobDetails jobDetails, StateName state);

    RecurringJob saveRecurringJob(RecurringJob recurringJob);

    List<RecurringJob> getRecurringJobs();

    int deleteRecurringJob(String id);

    JobStats getJobStats();

    void publishJobStatCounter(StateName state, int amount);

    default Job getJobById(JobId jobId) {
        return getJobById(jobId.asUUID());
    }
}
