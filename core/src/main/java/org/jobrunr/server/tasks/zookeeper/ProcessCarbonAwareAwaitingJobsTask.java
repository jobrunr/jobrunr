package org.jobrunr.server.tasks.zookeeper;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.carbonaware.CarbonAwareJobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

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
        // TODO should we run another task in case carbonAwareJobManager is not enabled and add a dashboard notification to alert if carbon aware jobs are in DB?
        if (carbonAwareJobManager == null) return;
        processManyJobs(this::getCarbonAwareAwaitingJobs,
                this::moveCarbonAwareJobToNextState,
                amountProcessed -> LOGGER.debug("Moved {} carbon aware jobs to next state", amountProcessed));
    }

    private List<Job> getCarbonAwareAwaitingJobs(List<Job> previousResults) {
        if (previousResults != null && previousResults.size() < pageRequestSize) return emptyList();
        return storageProvider.getCarbonAwareJobList(getDeadlineBeforeWhichToQueryCarbonAwareJobs(), ascOnCarbonAwareDeadline(pageRequestSize));
    }

    private void moveCarbonAwareJobToNextState(Job job) {
        carbonAwareJobManager.moveToNextState(job);
    }

    private Instant getDeadlineBeforeWhichToQueryCarbonAwareJobs() {
        return carbonAwareJobManager.getTheLaterBetweenForecastEndPeriodAndNextRefreshTime();
    }
}
