package org.jobrunr.server.tasks.zookeeper;

import org.jobrunr.jobs.states.StateName;
import org.jobrunr.server.BackgroundJobServer;

import static java.time.Instant.now;

public class DeleteDeletedJobsPermanentlyTask extends JobZooKeeperTask {

    public DeleteDeletedJobsPermanentlyTask(BackgroundJobServer backgroundJobServer) {
        super(backgroundJobServer);
    }

    @Override
    protected void runTask() {
        LOGGER.trace("Looking for deleted jobs that can be deleted permanently...");
        int totalAmountOfPermanentlyDeletedJobs = storageProvider.deleteJobsPermanently(StateName.DELETED, now().minus(backgroundJobServerConfiguration().getDeleteSucceededJobsAfter()));
        LOGGER.debug("Found {} deleted jobs that were permanently deleted as part of JobRunr maintenance", totalAmountOfPermanentlyDeletedJobs);
    }
}
