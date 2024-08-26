package org.jobrunr.server.tasks.zookeeper;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.RecurringJobsResult;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.PROCESSING;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.jobrunr.utils.CollectionUtils.getLast;

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

        List<RecurringJob> recurringJobs = getRecurringJobs();
        convertAndProcessManyJobs(recurringJobs,
                this::toScheduledJobs,
                totalAmountOfJobs -> LOGGER.debug("Found {} jobs to schedule from {} recurring jobs", totalAmountOfJobs, recurringJobs.size()));
    }

    private List<RecurringJob> getRecurringJobs() {
        if (storageProvider.recurringJobsUpdated(recurringJobs.getLastModifiedHash())) {
            this.recurringJobs = storageProvider.getRecurringJobs();
        }
        return this.recurringJobs;
    }

    List<Job> toScheduledJobs(RecurringJob recurringJob) {
        Instant from = getSchedulingWindowLowerBound(recurringJob);
        Instant upUntil = getSchedulingWindowUpperBound();

        List<Job> jobsToSchedule = getJobsToSchedule(recurringJob, from, upUntil);

        if (jobsToSchedule.isEmpty()) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Recurring job '{}' resulted in 0 scheduled job in time range {} - {} ({}).", recurringJob.getJobName(), from, upUntil, Duration.between(from, upUntil));
            }
        } else if (isAlreadyScheduledEnqueuedOrProcessing(recurringJob)) {
            LOGGER.info("Recurring job '{}' resulted in {} scheduled jobs in time range {} - {} ({}) but it is already SCHEDULED, ENQUEUED or PROCESSING. Run will be skipped as job is taking longer than given CronExpression or Interval.", recurringJob.getJobName(), jobsToSchedule.size(), from, upUntil, Duration.between(from, upUntil));
            jobsToSchedule.clear();
        } else if (jobsToSchedule.size() > 1) {
            LOGGER.info("Recurring job '{}' resulted in {} scheduled jobs in time range {} - {} ({}). This means either it's schedule is smaller than the pollInterval, your server was down or a long GC happened and JobRunr is catching up.", recurringJob.getJobName(), jobsToSchedule.size(), from, upUntil, Duration.between(from, upUntil));
        } else {
            if (LOGGER.isTraceEnabled()) {
                if (((ScheduledState) jobsToSchedule.get(0).getJobState()).getScheduledAt().isAfter(upUntil)) {
                    LOGGER.trace("Recurring job '{}' resulted in 1 job scheduled ahead of time.", recurringJob.getJobName());
                } else {
                    LOGGER.trace("Recurring job '{}' resulted in 1 scheduled job in time range {} - {} ({}).", recurringJob.getJobName(), from, upUntil, Duration.between(from, upUntil));
                }
            }
        }

        Job latestScheduledJob = getLast(jobsToSchedule);
        if (latestScheduledJob != null) {
            registerRecurringJobRun(recurringJob, latestScheduledJob);
        } else {
            registerRecurringJobRun(recurringJob, upUntil);
        }

        return jobsToSchedule;
    }

    private List<Job> getJobsToSchedule(RecurringJob recurringJob, Instant from, Instant upUntil) {
        if (from.isAfter(upUntil)) return emptyList();
        return recurringJob.toScheduledJobs(from, upUntil);
    }

    private Instant getSchedulingWindowLowerBound(RecurringJob recurringJob) {
        return recurringJobRuns.getOrDefault(recurringJob.getId(), runStartTime());
    }

    private Instant getSchedulingWindowUpperBound() {
        return runStartTime().plus(backgroundJobServerConfiguration().getPollInterval());
    }

    private boolean isAlreadyScheduledEnqueuedOrProcessing(RecurringJob recurringJob) {
        return storageProvider.recurringJobExists(recurringJob.getId(), SCHEDULED, ENQUEUED, PROCESSING);
    }

    private void registerRecurringJobRun(RecurringJob recurringJob, Job latestScheduledJob) {
        registerRecurringJobRun(recurringJob, ((ScheduledState) latestScheduledJob.getJobState()).getScheduledAt());
    }

    private void registerRecurringJobRun(RecurringJob recurringJob, Instant upUntil) {
        recurringJobRuns.put(recurringJob.getId(), upUntil);
    }
}
