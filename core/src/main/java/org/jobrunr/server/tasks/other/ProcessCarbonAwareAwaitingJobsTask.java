package org.jobrunr.server.tasks.other;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.tasks.zookeeper.AbstractJobZooKeeperTask;
import org.jobrunr.utils.carbonaware.CarbonAwareJobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.jobrunr.jobs.states.StateName.AWAITING;
import static org.jobrunr.storage.Paging.AmountBasedList.ascOnCarbonAwareDeadline;
import static org.jobrunr.storage.Paging.AmountBasedList.ascOnUpdatedAt;

public class ProcessCarbonAwareAwaitingJobsTask extends AbstractJobZooKeeperTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessCarbonAwareAwaitingJobsTask.class);

    private static final int DEFAULT_HOURS_AHEAD_TO_CHECK = 30;
    private final CarbonAwareJobManager carbonAwareJobManager;
    private final int hoursAheadToCheck;

    private final int pageRequestSize;

    public ProcessCarbonAwareAwaitingJobsTask(BackgroundJobServer backgroundJobServer) {
        super(backgroundJobServer);
        this.carbonAwareJobManager = backgroundJobServer.getCarbonAwareJobManager();
        this.pageRequestSize = backgroundJobServer.getConfiguration().getCarbonAwareJobsRequestSize();
        hoursAheadToCheck = DEFAULT_HOURS_AHEAD_TO_CHECK;
    }

    @Override
    protected void runTask() {
        carbonAwareJobManager.updateDayAheadEnergyPrices();
        processManyJobs(this::getCarbonAwareAwaitingJobs,
                this::moveCarbonAwareJobToNextState,
                amountProcessed -> LOGGER.info("Moved {} carbon aware jobs to next state", amountProcessed));
    }

    private List<Job> getCarbonAwareAwaitingJobs(List<Job> previousResults) {
        if (previousResults != null && previousResults.size() < pageRequestSize) return emptyList();
        return storageProvider.getCarbonAwareJobList(Instant.now().plus(hoursAheadToCheck, ChronoUnit.HOURS), ascOnCarbonAwareDeadline(pageRequestSize));
    }

    private void moveCarbonAwareJobToNextState(Job job) {
        carbonAwareJobManager.moveToNextState(job);
    }
}
