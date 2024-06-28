package org.jobrunr.server.tasks.other;

import org.jobrunr.carbonaware.CarbonAwareJobManager;
import org.jobrunr.jobs.Job;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.tasks.zookeeper.AbstractJobZooKeeperTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static java.time.LocalTime.MAX;
import static java.util.Collections.emptyList;
import static org.jobrunr.storage.Paging.AmountBasedList.ascOnCarbonAwareDeadline;

// TODO should this class be under zookeeper?
// TODO should this task be concerned about the pollInterval?
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
        return LocalDate.now().atTime(MAX).plusDays(1).atZone(carbonAwareJobManager.getTimeZone()).toInstant();
    }
}
