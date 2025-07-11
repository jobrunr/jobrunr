package org.jobrunr.server.tasks.zookeeper;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.states.SchedulableState;
import org.jobrunr.scheduling.Schedule;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.RecurringJobsResult;
import org.jobrunr.utils.InstantUtils;

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
import static org.jobrunr.utils.InstantUtils.isInstantBeforeOrEqualTo;

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
        List<RecurringJob> recurringJobsToRun = getRecurringJobsToRun();

        if (this.recurringJobRuns.isEmpty()) {
            fillRecurringJobRunsWithLatestScheduledAtForCarbonAware();
        }

        convertAndProcessManyJobs(recurringJobsToRun,
                recurringJob -> toJobsToSchedule(recurringJob, from, upUntil),
                totalAmountOfJobs -> LOGGER.debug("Found {} jobs to schedule from {} recurring jobs", totalAmountOfJobs, recurringJobsToRun.size()));
    }

    private void fillRecurringJobRunsWithLatestScheduledAtForCarbonAware() {
        for (RecurringJob recurringJob : recurringJobs) {
            Schedule schedule = recurringJob.getSchedule();
            if (schedule.isNotCarbonAware()) continue;

            Instant scheduledAt = getLatestScheduledAtOfJobsInStorageProviderForAnyState(recurringJob);
            if (scheduledAt == null) continue;

            Instant nextRun = recurringJob.getNextRun(runStartTime());
            if (isInstantBeforeOrEqualTo(nextRun.minus(schedule.getCarbonAwareScheduleMargin().getMarginBefore()), scheduledAt)) {
                this.recurringJobRuns.put(recurringJob.getId(), nextRun);
            }
        }
    }

    private List<RecurringJob> getRecurringJobsToRun() {
        if (storageProvider.recurringJobsUpdated(recurringJobs.getLastModifiedHash())) {
            this.recurringJobs = storageProvider.getRecurringJobs();
        }
        return this.recurringJobs;
    }

    private List<Job> toJobsToSchedule(RecurringJob recurringJob, Instant from, Instant upUntil) {
        List<Job> jobsToSchedule = createJobsToSchedule(recurringJob, from, upUntil);
        if (jobsToSchedule.isEmpty()) {
            LOGGER.trace("Recurring job '{}' resulted in 0 scheduled jobs.", recurringJob.getJobName());
            return emptyList();
        }

        Instant scheduledAtOfLastJobToSchedule = getScheduledAtOfLastScheduledJob(jobsToSchedule);
        Instant scheduledAtOfLatestJobInStorageProvider = getLatestScheduledAtOfJobsInStorageProvider(recurringJob);

        if (hasJobInQueueOrProcessing(scheduledAtOfLatestJobInStorageProvider)) {
            if (recurringJobIsTakingTooLong(upUntil, scheduledAtOfLastJobToSchedule)) {
                LOGGER.info("Recurring job '{}' resulted in {} scheduled jobs in time range {} - {} ({}) but it is already AWAITING, SCHEDULED, ENQUEUED or PROCESSING. Run will be skipped as job is taking longer than given CronExpression or Interval.", recurringJob.getJobName(), jobsToSchedule.size(), from, upUntil, Duration.between(from, upUntil));
            }
            jobsToSchedule.clear();
            scheduledAtOfLastJobToSchedule = InstantUtils.max(upUntil, scheduledAtOfLatestJobInStorageProvider);
        }

        if (jobsToSchedule.size() > 1) {
            LOGGER.info("Recurring job '{}' resulted in {} scheduled jobs in time range {} - {} ({}). This means either its schedule is smaller than the pollInterval, your server was down or a long GC happened and JobRunr is catching up.", recurringJob.getJobName(), jobsToSchedule.size(), from, upUntil, Duration.between(from, upUntil));
        } else if (jobsToSchedule.size() == 1) {
            LOGGER.debug("Recurring job '{}' resulted in 1 scheduled job.", recurringJob.getJobName());
        }
        registerRecurringJobRun(recurringJob, scheduledAtOfLastJobToSchedule);
        return jobsToSchedule;
    }

    private List<Job> createJobsToSchedule(RecurringJob recurringJob, Instant runStartTime, Instant upUntil) {
        //Instant lastRun = recurringJobRuns.getOrDefault(recurringJob.getId(), runStartTime);
        Instant lastRun = recurringJobRuns.get(recurringJob.getId());
        if (lastRun == null) {
            return recurringJob.toScheduledJobs(runStartTime, upUntil);
        } else if (lastRun.isAfter(runStartTime)) {
            return emptyList(); // already scheduled ahead of time
        } else {
            return recurringJob.toScheduledJobs(lastRun.plusMillis(1), upUntil); // as recurringJob.toScheduledJobs from is inclusive
        }
    }

    private boolean hasJobInQueueOrProcessing(Instant latestScheduledAt) {
        return latestScheduledAt != null;
    }

    private static boolean recurringJobIsTakingTooLong(Instant upUntil, Instant scheduledAtOfLastJobToSchedule) {
        return scheduledAtOfLastJobToSchedule.isBefore(upUntil);
    }

    private Instant getLatestScheduledAtOfJobsInStorageProvider(RecurringJob recurringJob) {
        return storageProvider.getRecurringJobLatestScheduledInstant(recurringJob.getId(), AWAITING, SCHEDULED, ENQUEUED, PROCESSING);
    }

    private Instant getLatestScheduledAtOfJobsInStorageProviderForAnyState(RecurringJob recurringJob) {
        return storageProvider.getRecurringJobLatestScheduledInstant(recurringJob.getId());
    }

    private void registerRecurringJobRun(RecurringJob recurringJob, Instant instant) {
        recurringJobRuns.put(recurringJob.getId(), instant);
    }

    private Instant getScheduledAtOfLastScheduledJob(List<Job> jobsToSchedule) {
        return findLast(jobsToSchedule)
                .map(x -> ((SchedulableState) x.getJobState()).getScheduledAt())
                .orElseThrow(() -> new IllegalArgumentException("jobsToSchedule must not be empty."));
    }
}
