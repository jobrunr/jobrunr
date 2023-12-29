package org.jobrunr.server.zookeeper.tasks;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.JobZooKeeper;
import org.jobrunr.server.strategy.WorkDistributionStrategy;
import org.jobrunr.storage.navigation.AmountRequest;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class OnboardNewWorkTask extends ZooKeeperTask {

    private final ReentrantLock reentrantLock;
    private final WorkDistributionStrategy workDistributionStrategy;

    public OnboardNewWorkTask(JobZooKeeper jobZooKeeper, BackgroundJobServer backgroundJobServer) {
        super(jobZooKeeper, backgroundJobServer);
        this.reentrantLock = new ReentrantLock();
        this.workDistributionStrategy = backgroundJobServer.getWorkDistributionStrategy();
    }

    @Override
    protected void runTask() {
        if (backgroundJobServer.isRunning() && reentrantLock.tryLock()) {
            try {
                LOGGER.trace("Looking for enqueued jobs... ");
                final AmountRequest workPageRequest = workDistributionStrategy.getWorkPageRequest();
                if (workPageRequest.getLimit() > 0) {
                    final List<Job> enqueuedJobs = storageProvider.getJobsToProcess(backgroundJobServer, workPageRequest);
                    enqueuedJobs.forEach(backgroundJobServer::processJob);
                    LOGGER.debug("Found {} enqueued jobs to process.", enqueuedJobs.size());
                }
            } finally {
                reentrantLock.unlock();
            }
        }
    }
}
