package org.jobrunr.server.tasks.zookeeper;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.states.Schedulable;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.RecurringJobsResult;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.jobrunr.jobs.states.StateName.AWAITING;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.PROCESSING;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.jobrunr.utils.CollectionUtils.findLast;

public class ProcessRecurringJobsTask extends AbstractJobZooKeeperTask {

    private final Map<String, Instant> recurringJobRuns;
    private RecurringJobsResult recurringJobs;

    public ProcessRecurringJobsTask(BackgroundJobServer backgroundJobServer) {
        super(backgroundJobServer);
        this.recurringJobRuns = new HashMap<>(storageProvider.getRecurringJobsLatestScheduledRun());
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
            LOGGER.info("Recurring job '{}' resulted in {} scheduled jobs in time range {} - {} ({}). This means either its schedule is smaller than the pollInterval, your server was down or a long GC happened and JobRunr is catching up.", recurringJob.getJobName(), jobsToSchedule.size(), from, upUntil, Duration.between(from, upUntil));
        } else if (isAlreadyInOneOfTheScheduledStates(recurringJob)) {
            LOGGER.info("Recurring job '{}' resulted in {} scheduled jobs in time range {} - {} ({}) but it is already either AWAITING/SCHEDULED/ENQUEUED or PROCESSING and taking longer than given CronExpression or Interval. Run will be skipped.", recurringJob.getJobName(), jobsToSchedule.size(), from, upUntil, Duration.between(from, upUntil));
            jobsToSchedule.clear();
        } else if (jobsToSchedule.size() == 1) {
            LOGGER.debug("Recurring job '{}' resulted in 1 scheduled job.", recurringJob.getJobName());
        }
        registerRecurringJobRun(recurringJob, upUntil, jobsToSchedule);
        return jobsToSchedule;
    }

    private List<Job> getJobsToSchedule(RecurringJob recurringJob, Instant runStartTime, Instant upUntil) {
        Instant lastRun = recurringJobRuns.getOrDefault(recurringJob.getId(), runStartTime);
        if (lastRun.isAfter(runStartTime)) return emptyList(); // already scheduled ahead of time
        return recurringJob.toScheduledJobs(lastRun, upUntil);
    }

    private boolean isAlreadyInOneOfTheScheduledStates(RecurringJob recurringJob) {
        return storageProvider.recurringJobExists(recurringJob.getId(), AWAITING, SCHEDULED, ENQUEUED, PROCESSING);
    }

    private void registerRecurringJobRun(RecurringJob recurringJob, Instant upUntil, List<Job> scheduledJobs) {
        Instant instant = findLast(scheduledJobs).map(x -> ((Schedulable) x.getJobState()).getScheduledAt()).orElse(upUntil);
        recurringJobRuns.put(recurringJob.getId(), instant);
    }
}
