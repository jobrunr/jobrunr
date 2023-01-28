package org.jobrunr.server.zookeeper.tasks;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.JobZooKeeper;

import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

import static java.time.Instant.now;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;
import static org.jobrunr.storage.PageRequest.ascOnUpdatedAt;

public class DeleteSucceededJobsTask extends ZooKeeperTask {

    private final int pageRequestSize;

    public DeleteSucceededJobsTask(JobZooKeeper jobZooKeeper, BackgroundJobServer backgroundJobServer) {
        super(jobZooKeeper, backgroundJobServer);
        this.pageRequestSize = backgroundJobServer.getConfiguration().getSucceededJobsRequestSize();
    }

    @Override
    protected void runTask() {
        LOGGER.trace("Looking for succeeded jobs that can go to the deleted state... ");
        final Instant updatedBefore = now().minus(serverStatus().getDeleteSucceededJobsAfter());
        Supplier<List<Job>> succeededJobsSupplier = () -> storageProvider.getJobs(SUCCEEDED, updatedBefore, ascOnUpdatedAt(pageRequestSize));
        processJobList(succeededJobsSupplier, job -> job.delete("JobRunr maintenance - deleting succeeded job"), this::handleTotalAmountOfSucceededJobs);
    }

    private void handleTotalAmountOfSucceededJobs(int totalAmountOfSucceededJobs) {
        if (totalAmountOfSucceededJobs > 0) {
            storageProvider.publishTotalAmountOfSucceededJobs(totalAmountOfSucceededJobs);
        }
        LOGGER.debug("Found {} succeeded jobs that moved to DELETED state as part of JobRunr maintenance", totalAmountOfSucceededJobs);
    }
}
