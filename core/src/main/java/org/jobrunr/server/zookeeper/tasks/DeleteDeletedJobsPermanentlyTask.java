package org.jobrunr.server.zookeeper.tasks;

import org.jobrunr.jobs.states.StateName;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.JobZooKeeper;

import static java.time.Instant.now;

public class DeleteDeletedJobsPermanentlyTask extends ZooKeeperTask {

    public DeleteDeletedJobsPermanentlyTask(JobZooKeeper jobZooKeeper, BackgroundJobServer backgroundJobServer) {
        super(jobZooKeeper, backgroundJobServer);
    }

    @Override
    protected void runTask() {
        LOGGER.trace("Looking for deleted jobs that can be deleted permanently...");
        int totalAmountOfPermanentlyDeletedJobs = storageProvider.deleteJobsPermanently(StateName.DELETED, now().minus(serverStatus().getDeleteSucceededJobsAfter()));
        LOGGER.debug("Found {} deleted jobs that were permanently deleted as part of JobRunr maintenance", totalAmountOfPermanentlyDeletedJobs);
    }
}
