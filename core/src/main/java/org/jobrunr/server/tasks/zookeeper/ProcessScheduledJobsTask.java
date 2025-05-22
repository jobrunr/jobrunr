package org.jobrunr.server.tasks.zookeeper;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.BackgroundJobServer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static java.time.Instant.now;
import static java.util.Collections.emptyList;
import static org.jobrunr.storage.Paging.AmountBasedList.ascOnUpdatedAt;

public class ProcessScheduledJobsTask extends AbstractJobZooKeeperTask {

    private final int pageRequestSize;

    public ProcessScheduledJobsTask(BackgroundJobServer backgroundJobServer) {
        super(backgroundJobServer);
        this.pageRequestSize = backgroundJobServer.getConfiguration().getScheduledJobsRequestSize();
    }

    @Override
    protected void runTask() {
        LOGGER.trace("Looking for scheduled jobs... ");
        //Instant scheduledBefore = now().plus(backgroundJobServerConfiguration().getPollInterval());
        processManyJobs(previousResults -> getJobsToSchedule(now().plus(20, ChronoUnit.HOURS), previousResults),
                Job::enqueue,
                totalAmountOfEnqueuedJobs -> LOGGER.debug("Found {} scheduled jobs to enqueue.", totalAmountOfEnqueuedJobs));
    }

    private List<Job> getJobsToSchedule(Instant scheduledBefore, List<Job> previousResults) {
        if (previousResults != null && previousResults.size() < pageRequestSize) return emptyList();
        return storageProvider.getScheduledJobs(scheduledBefore, ascOnUpdatedAt(pageRequestSize));
    }
}
