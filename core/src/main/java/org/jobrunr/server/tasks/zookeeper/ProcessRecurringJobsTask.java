package org.jobrunr.server.tasks.zookeeper;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.states.CarbonAwareAwaitingState;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.RecurringJobsResult;
import org.jobrunr.utils.InstantUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.jobrunr.utils.CollectionUtils.findLast;

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
        // TODO remove afterwards; simulate server reboot
        recurringJobRuns.clear();
        recurringJobs = new RecurringJobsResult();
        // ---

        List<Job> jobsToSchedule = createJobsToSchedule(recurringJob, from, upUntil);
        if (jobsToSchedule.isEmpty()) {
            LOGGER.trace("Recurring job '{}' resulted in 0 scheduled job.", recurringJob.getJobName());
            return emptyList();
        }

        Instant scheduledAtOfLastJobToSchedule = getLastScheduledAtOf(jobsToSchedule);
        Instant scheduledAtOfLatestJobInDb = getLatestScheduledAtOfJobsInStorageProvider(recurringJob);

        // TODO write another unit test to cover the special cases
        if (hasJobInQueueOrProcessing(scheduledAtOfLastJobToSchedule, scheduledAtOfLatestJobInDb)) {
            if (recurringJobIsTakingTooLong(upUntil, scheduledAtOfLastJobToSchedule)) {
                LOGGER.info("Recurring job '{}' resulted in {} scheduled jobs in time range {} - {} ({}) but it is already AWAITING, SCHEDULED, ENQUEUED or PROCESSING. Run will be skipped as job is taking longer than given CronExpression or Interval.", recurringJob.getJobName(), jobsToSchedule.size(), from, upUntil, Duration.between(from, upUntil));
            }
            jobsToSchedule.clear();
            scheduledAtOfLastJobToSchedule = InstantUtils.max(scheduledAtOfLastJobToSchedule, scheduledAtOfLatestJobInDb, upUntil);
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
        Instant lastRun = recurringJobRuns.getOrDefault(recurringJob.getId(), runStartTime);
        if (lastRun.isAfter(runStartTime)) return emptyList(); // already scheduled ahead of time
        return recurringJob.toScheduledJobs(lastRun, upUntil);
    }

    private boolean hasJobInQueueOrProcessing(Instant latestScheduledAt, Instant scheduledAtOfLatestJobInDb) {
        return scheduledAtOfLatestJobInDb != null && InstantUtils.isInstantBeforeOrEqualTo(latestScheduledAt, scheduledAtOfLatestJobInDb);
    }

    private static boolean recurringJobIsTakingTooLong(Instant upUntil, Instant scheduledAtOfLastJobToSchedule) {
        return scheduledAtOfLastJobToSchedule.isBefore(upUntil);
    }

    private Instant getLatestScheduledAtOfJobsInStorageProvider(RecurringJob recurringJob) {
        return InstantUtils.max(storageProvider.getRecurringJobScheduledInstants(recurringJob.getId()));
    }

    private void registerRecurringJobRun(RecurringJob recurringJob, Instant instant) {
        recurringJobRuns.put(recurringJob.getId(), instant);
    }

    private Instant getLastScheduledAtOf(List<Job> jobs) {
        return findLast(jobs)
                .map(x -> getScheduledAt(x))
                .orElseThrow(() -> new IllegalArgumentException("jobs must not be empty."));
    }

    private static Instant getScheduledAt(Job job) {
        if(job.getJobState() instanceof ScheduledState) {
            return ((ScheduledState) job.getJobState()).getScheduledAt();
        } else if(job.getJobState() instanceof CarbonAwareAwaitingState) {
            return ((CarbonAwareAwaitingState) job.getJobState()).getDeadline();
        }
        throw new IllegalArgumentException("job has no valid state with a possible scheduledAt.");
    }
}
