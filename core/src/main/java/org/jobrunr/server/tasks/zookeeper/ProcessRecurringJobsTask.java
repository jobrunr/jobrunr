package org.jobrunr.server.tasks.zookeeper;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.states.SchedulableState;
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

public class ProcessRecurringJobsTask extends AbstractJobZooKeeperTask {

    private final Map<String, Instant> recurringJobRuns;
    private RecurringJobsResult recurringJobs;

    public ProcessRecurringJobsTask(BackgroundJobServer backgroundJobServer) {
        super(backgroundJobServer);
        this.recurringJobRuns = new HashMap<>();

        // TODO fill recurring job runs just like in pro
        // TODO write integration-like test perhaps using same wiremock thing as in BackgroundJobByJobLambdaTest (effectively kill filter instead of whitebox set map back to empty)

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
        List<Job> jobsToSchedule = createJobsToSchedule(recurringJob, from, upUntil);
        if (jobsToSchedule.isEmpty()) {
            LOGGER.trace("Recurring job '{}' resulted in 0 scheduled job.", recurringJob.getJobName());
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
        Instant lastRun = recurringJobRuns.getOrDefault(recurringJob.getId(), runStartTime);
        if (lastRun.isAfter(runStartTime)) return emptyList(); // already scheduled ahead of time
        return recurringJob.toScheduledJobs(lastRun, upUntil);
    }

    private boolean hasJobInQueueOrProcessing(Instant latestScheduledAt) {
        return latestScheduledAt != null;
    }

    private static boolean recurringJobIsTakingTooLong(Instant upUntil, Instant scheduledAtOfLastJobToSchedule) {
        return scheduledAtOfLastJobToSchedule.isBefore(upUntil);
    }

    private Instant getLatestScheduledAtOfJobsInStorageProvider(RecurringJob recurringJob) {
        return InstantUtils.max(storageProvider.getRecurringJobScheduledInstants(recurringJob.getId(), AWAITING, SCHEDULED, ENQUEUED, PROCESSING));
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
