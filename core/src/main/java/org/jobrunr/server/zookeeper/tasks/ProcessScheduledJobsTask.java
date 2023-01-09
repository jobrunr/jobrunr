package org.jobrunr.server.zookeeper.tasks;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.JobZooKeeper;

import java.util.List;
import java.util.function.Supplier;

import static java.time.Instant.now;
import static org.jobrunr.storage.PageRequest.ascOnUpdatedAt;

public class ProcessScheduledJobsTask extends ZooKeeperTask {

    private final int pageRequestSize;

    public ProcessScheduledJobsTask(JobZooKeeper jobZooKeeper, BackgroundJobServer backgroundJobServer) {
        super(jobZooKeeper, backgroundJobServer);
        this.pageRequestSize = backgroundJobServer.getConfiguration().getScheduledJobsRequestSize();
    }

    @Override
    protected void runTask() {
        LOGGER.trace("Looking for scheduled jobs... ");
        Supplier<List<Job>> scheduledJobsSupplier = () -> storageProvider.getScheduledJobs(now().plusSeconds(serverStatus().getPollIntervalInSeconds()), ascOnUpdatedAt(pageRequestSize));
        processJobList(scheduledJobsSupplier, Job::enqueue, totalAmountOfEnqueuedJobs -> LOGGER.debug("Found {} scheduled jobs to enqueue.", totalAmountOfEnqueuedJobs));
    }
}
