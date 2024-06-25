package org.jobrunr.server.tasks.zookeeper;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.states.CarbonAwareAwaitingState;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.carbonaware.CarbonAwareJobManager;
import org.jobrunr.storage.RecurringJobsResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.jobrunr.JobRunrException.shouldNotHappenException;
import static org.jobrunr.jobs.states.StateName.AWAITING;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.PROCESSING;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.jobrunr.utils.CollectionUtils.getLast;

public class ProcessRecurringJobsTask extends AbstractJobZooKeeperTask {

    private final Map<String, Instant> recurringJobRuns;
    private final CarbonAwareJobManager carbonAwareJobManager;
    private RecurringJobsResult recurringJobs;

    public ProcessRecurringJobsTask(BackgroundJobServer backgroundJobServer) {
        super(backgroundJobServer);
        this.recurringJobRuns = storageProvider.getRecurringJobsLatestScheduledRun();
        this.carbonAwareJobManager = backgroundJobServer.getCarbonAwareJobManager();
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

    private List<Job> toScheduledJobs(RecurringJob recurringJob) {
        List<Job> jobsToCreate = getJobsToCreate(recurringJob);
        if (jobsToCreate.isEmpty()) {
            LOGGER.trace("Recurring job '{}' resulted in 0 scheduled job because it is already scheduled.", recurringJob.getJobName());
        } else if (isAlreadyAwaitingScheduledEnqueuedOrProcessing(recurringJob)) {
            LOGGER.debug("Recurring job '{}' is already awaiting, scheduled, enqueued or processing. Run will be skipped as job is taking longer than given CronExpression or Interval.", recurringJob.getJobName());
            jobsToCreate.clear();
        }
        if (jobsToCreate.size() > 1) {
            LOGGER.info("Recurring job '{}' resulted in {} scheduled jobs. This means a long GC happened and JobRunr is catching up.", recurringJob.getJobName(), jobsToCreate.size());
        } else if (jobsToCreate.size() == 1) {
            LOGGER.debug("Recurring job '{}' resulted in 1 scheduled job.", recurringJob.getJobName());
        }

        jobsToCreate.stream()
                .filter(job -> job.getJobState() instanceof CarbonAwareAwaitingState)
                .forEach(carbonAwareJobManager::moveToNextState);

        registerRecurringJobRun(recurringJob, getLast(jobsToCreate));
        return jobsToCreate;
    }

    private List<Job> getJobsToCreate(RecurringJob recurringJob) {
        Instant lastScheduledRun = recurringJobRuns.getOrDefault(recurringJob.getId(), runStartTime());
        Instant upUntil = runStartTime().plus(backgroundJobServerConfiguration().getPollInterval());
        if (lastScheduledRun.isAfter(upUntil)) {
            return emptyList();
        }
        return recurringJob.toJobsWith1FutureRun(lastScheduledRun, upUntil);
    }

    private boolean isAlreadyAwaitingScheduledEnqueuedOrProcessing(RecurringJob recurringJob) {
        // TODO result should be compared to > 1?
        return storageProvider.countRecurringJobInstances(recurringJob.getId(), AWAITING, SCHEDULED, ENQUEUED, PROCESSING) > 0;
    }

    private void registerRecurringJobRun(RecurringJob recurringJob, Job nextJob) {
        if (nextJob == null) return;

        JobState jobState = nextJob.getJobState();
        if (jobState instanceof CarbonAwareAwaitingState) {
            registerRecurringJobRun(recurringJob, ((CarbonAwareAwaitingState) jobState).getPreferredInstant());
        } else if (jobState instanceof ScheduledState) {
            registerRecurringJobRun(recurringJob, ((ScheduledState) jobState).getScheduledAt());
        } else {
            throw shouldNotHappenException("Recurring job '" + recurringJob.getJobName() + "' has unsupported job state '" + jobState + "'.");
        }
    }

    private void registerRecurringJobRun(RecurringJob recurringJob, Instant upUntil) {
        recurringJobRuns.put(recurringJob.getId(), upUntil);
    }
}
