package org.jobrunr.server.zookeeper.tasks;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.JobZooKeeper;
import org.jobrunr.storage.RecurringJobsResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jobrunr.jobs.states.StateName.*;
import static org.jobrunr.utils.CollectionUtils.isNotNullOrEmpty;

public class ProcessRecurringJobsTask extends ZooKeeperTask {

    private final Map<String, Instant> recurringJobRuns;
    private RecurringJobsResult recurringJobs;

    public ProcessRecurringJobsTask(JobZooKeeper jobZooKeeper, BackgroundJobServer backgroundJobServer) {
        super(jobZooKeeper, backgroundJobServer);
        this.recurringJobRuns = new HashMap<>();
        this.recurringJobs = new RecurringJobsResult();
    }

    @Override
    protected void runTask() {
        LOGGER.trace("Looking for recurring jobs... ");
        List<RecurringJob> recurringJobs = getRecurringJobs();
        processRecurringJobs(recurringJobs);
    }

    private List<RecurringJob> getRecurringJobs() {
        if(storageProvider.recurringJobsUpdated(recurringJobs.getLastModifiedHash())) {
            this.recurringJobs = storageProvider.getRecurringJobs();
        }
        return this.recurringJobs;
    }

    void processRecurringJobs(List<RecurringJob> recurringJobs) {
        LOGGER.debug("Found {} recurring jobs.", recurringJobs.size());

        Instant from = runStartTime();
        Instant upUntil = runStartTime().plusSeconds(serverStatus().getPollIntervalInSeconds());

        List<Job> allJobsToSchedule = new ArrayList<>();
        for (RecurringJob recurringJob : recurringJobs) {
            List<Job> jobsToSchedule = getJobsToSchedule(recurringJob, from, upUntil);
            if(jobsToSchedule.isEmpty()) {
                LOGGER.trace("Recurring job '{}' resulted in 0 scheduled job.", recurringJob.getJobName());
            } else if(jobsToSchedule.size() > 1) {
                LOGGER.info("Recurring job '{}' resulted in {} scheduled jobs. This means a long GC happened and JobRunr is catching up.", recurringJob.getJobName(), jobsToSchedule.size());
                allJobsToSchedule.addAll(jobsToSchedule);
            } else if(isAlreadyScheduledEnqueuedOrProcessing(recurringJob)) {
                LOGGER.debug("Recurring job '{}' is already scheduled, enqueued or processing. Run will be skipped as job is taking longer than given CronExpression or Interval.", recurringJob.getJobName());
            } else if(jobsToSchedule.size() == 1) {
                LOGGER.debug("Recurring job '{}' resulted in 1 scheduled job.", recurringJob.getJobName());
                allJobsToSchedule.addAll(jobsToSchedule);
            }
            registerRecurringJobRun(recurringJob, upUntil);
        }
        if(isNotNullOrEmpty(allJobsToSchedule)) {
            storageProvider.save(allJobsToSchedule);
        }
    }

    private List<Job> getJobsToSchedule(RecurringJob recurringJob, Instant runStartTime, Instant upUntil) {
        Instant lastRun = recurringJobRuns.getOrDefault(recurringJob.getId(), runStartTime);
        return recurringJob.toScheduledJobs(lastRun, upUntil);
    }

    private boolean isAlreadyScheduledEnqueuedOrProcessing(RecurringJob recurringJob) {
        return storageProvider.recurringJobExists(recurringJob.getId(), SCHEDULED, ENQUEUED, PROCESSING);
    }

    private void registerRecurringJobRun(RecurringJob recurringJob, Instant upUntil) {
        recurringJobRuns.put(recurringJob.getId(), upUntil);
    }
}
