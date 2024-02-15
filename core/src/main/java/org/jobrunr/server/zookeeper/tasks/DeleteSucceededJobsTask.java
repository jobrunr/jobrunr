package org.jobrunr.server.zookeeper.tasks;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.JobZooKeeper;

import java.time.Instant;
import java.util.List;

import static java.time.Instant.now;
import static java.util.Collections.emptyList;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;
import static org.jobrunr.storage.Paging.AmountBasedList.ascOnUpdatedAt;

public class DeleteSucceededJobsTask extends ZooKeeperTask {

    private final int pageRequestSize;

    public DeleteSucceededJobsTask(JobZooKeeper jobZooKeeper, BackgroundJobServer backgroundJobServer) {
        super(jobZooKeeper, backgroundJobServer);
        this.pageRequestSize = backgroundJobServer.getConfiguration().getSucceededJobsRequestSize();
    }

    @Override
    protected void runTask() {
        LOGGER.trace("Looking for succeeded jobs that can go to the deleted state... ");
        final Instant updatedBefore = now().minus(backgroundJobServerConfiguration().getDeleteSucceededJobsAfter());
        processManyJobs(previousResults -> getSucceededJobs(updatedBefore, previousResults),
                job -> job.delete("JobRunr maintenance - deleting succeeded job"),
                this::handleTotalAmountOfSucceededJobs);
    }

    private List<Job> getSucceededJobs(Instant updatedBefore, List<Job> previousResults) {
        if(previousResults != null && previousResults.size() < pageRequestSize) return emptyList();
        return storageProvider.getJobList(SUCCEEDED, updatedBefore, ascOnUpdatedAt(pageRequestSize));
    }

    private void handleTotalAmountOfSucceededJobs(int totalAmountOfSucceededJobs) {
        if (totalAmountOfSucceededJobs > 0) {
            storageProvider.publishTotalAmountOfSucceededJobs(totalAmountOfSucceededJobs);
        }
        LOGGER.debug("Found {} succeeded jobs that moved to DELETED state as part of JobRunr maintenance", totalAmountOfSucceededJobs);
    }
}
