package org.jobrunr.server.zookeeper.tasks;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.JobZooKeeper;

import java.time.Instant;
import java.util.List;

import static java.time.Instant.now;
import static java.util.Collections.emptyList;
import static org.jobrunr.storage.Paging.AmountBasedList.ascOnUpdatedAt;

public class ProcessScheduledJobsTask extends ZooKeeperTask {

    private final int pageRequestSize;

    public ProcessScheduledJobsTask(JobZooKeeper jobZooKeeper, BackgroundJobServer backgroundJobServer) {
        super(jobZooKeeper, backgroundJobServer);
        this.pageRequestSize = backgroundJobServer.getConfiguration().getScheduledJobsRequestSize();
    }

    @Override
    protected void runTask() {
        LOGGER.trace("Looking for scheduled jobs... ");
        Instant scheduledBefore = now().plus(backgroundJobServerConfiguration().getPollInterval());
        processManyJobs(previousResults -> getJobsToSchedule(scheduledBefore, previousResults),
                Job::enqueue,
                totalAmountOfEnqueuedJobs -> LOGGER.debug("Found {} scheduled jobs to enqueue.", totalAmountOfEnqueuedJobs));
    }

    private List<Job> getJobsToSchedule(Instant scheduledBefore, List<Job> previousResults) {
        if(previousResults != null && previousResults.size() < pageRequestSize) return emptyList();
        return storageProvider.getScheduledJobs(scheduledBefore, ascOnUpdatedAt(pageRequestSize));
    }
}
