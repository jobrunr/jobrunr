package org.jobrunr.server.tasks.zookeeper;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.RecurringJobsResult;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.PROCESSING;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;

public class ProcessRecurringJobsTask extends AbstractJobZooKeeperTask {

    private final Map<String, Instant> recurringJobRuns;
    private RecurringJobsResult recurringJobs;

    public ProcessRecurringJobsTask(BackgroundJobServer backgroundJobServer) {
        super(backgroundJobServer);
        this.recurringJobRuns = new HashMap<>();
        this.recurringJobs = new RecurringJobsResult();
    }

    @Override
    protected void runTask() {
        LOGGER.trace("Looking for recurring jobs... ");

        Instant from = runStartTime();
        Instant upUntil = runStartTime().plus(backgroundJobServerConfiguration().getPollInterval());
        List<RecurringJob> recurringJobs = getRecurringJobs();
        convertAndProcessManyJobs(recurringJobs,
                recurringJob -> toScheduledJobs(recurringJob, from, upUntil),
                totalAmountOfJobs -> LOGGER.debug("Found {} jobs to schedule from {} recurring jobs", totalAmountOfJobs, recurringJobs.size()));
    }

    private List<RecurringJob> getRecurringJobs() {
        if (storageProvider.recurringJobsUpdated(recurringJobs.getLastModifiedHash())) {
            this.recurringJobs = storageProvider.getRecurringJobs();
        }
        return this.recurringJobs;
    }

    List<Job> toScheduledJobs(RecurringJob recurringJob, Instant from, Instant upUntil) {
        List<Job> jobsToSchedule = getJobsToSchedule(recurringJob, from, upUntil);
        if (jobsToSchedule.isEmpty()) {
            LOGGER.trace("Recurring job '{}' resulted in 0 scheduled job.", recurringJob.getJobName());
        } else if (jobsToSchedule.size() > 1) {
            LOGGER.info("Recurring job '{}' resulted in {} scheduled jobs. This means a long GC happened and JobRunr is catching up.", recurringJob.getJobName(), jobsToSchedule.size());
        } else if (isAlreadyScheduledEnqueuedOrProcessing(recurringJob)) {
            LOGGER.debug("Recurring job '{}' is already scheduled, enqueued or processing. Run will be skipped as job is taking longer than given CronExpression or Interval.", recurringJob.getJobName());
            jobsToSchedule.clear();
        } else if (jobsToSchedule.size() == 1) {
            LOGGER.debug("Recurring job '{}' resulted in 1 scheduled job.", recurringJob.getJobName());
        }
        registerRecurringJobRun(recurringJob, upUntil);
        return jobsToSchedule;
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
