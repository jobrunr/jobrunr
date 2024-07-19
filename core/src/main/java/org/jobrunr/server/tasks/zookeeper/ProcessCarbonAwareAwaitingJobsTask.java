package org.jobrunr.server.tasks.zookeeper;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.CarbonAwareAwaitingState;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.carbonaware.CarbonAwareJobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Collections.emptyList;
import static org.jobrunr.storage.Paging.AmountBasedList.ascOnCarbonAwareDeadline;

public class ProcessCarbonAwareAwaitingJobsTask extends AbstractJobZooKeeperTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessCarbonAwareAwaitingJobsTask.class);

    private final CarbonAwareJobManager carbonAwareJobManager;
    private final int pageRequestSize;

    public ProcessCarbonAwareAwaitingJobsTask(BackgroundJobServer backgroundJobServer) {
        super(backgroundJobServer);
        this.carbonAwareJobManager = backgroundJobServer.getCarbonAwareJobManager();
        this.pageRequestSize = backgroundJobServer.getConfiguration().getCarbonAwareAwaitingJobsRequestSize();
    }

    @Override
    protected void runTask() {
        if (carbonAwareJobManager != null) carbonAwareJobManager.updateCarbonIntensityForecastIfNecessary();
        processManyJobs(this::getCarbonAwareAwaitingJobs,
                this::moveCarbonAwareJobToNextState,
                amountProcessed -> LOGGER.debug("Moved {} carbon aware jobs to next state", amountProcessed));
    }

    private List<Job> getCarbonAwareAwaitingJobs(List<Job> previousResults) {
        if (previousResults != null && previousResults.size() < pageRequestSize) return emptyList();
        return storageProvider.getCarbonAwareJobList(getDeadlineBeforeWhichToQueryCarbonAwareJobs(), ascOnCarbonAwareDeadline(pageRequestSize));
    }

    private void moveCarbonAwareJobToNextState(Job job) {
        if (carbonAwareJobManager == null) {
            moveJobToScheduledAsCarbonAwareSchedulingIsNotEnabled(job);
        } else {
            carbonAwareJobManager.moveToNextState(job);
        }
    }

    private Instant getDeadlineBeforeWhichToQueryCarbonAwareJobs() {
        return carbonAwareJobManager == null
                ? now().plus(365, DAYS)
                : carbonAwareJobManager.getAvailableForecastEndTime().minusNanos(1);
    }

    private void moveJobToScheduledAsCarbonAwareSchedulingIsNotEnabled(Job job) {
        CarbonAwareAwaitingState carbonAwareAwaitingState = job.getJobState();
        Instant scheduleAt = carbonAwareAwaitingState.getPreferredInstant() != null
                ? carbonAwareAwaitingState.getPreferredInstant()
                : carbonAwareAwaitingState.getFrom();
        carbonAwareAwaitingState.moveToNextState(job, scheduleAt, "Carbon aware scheduling is not enabled. Job will be scheduled at pre-defined preferred instant.");
    }
}
