package org.jobrunr.server.zookeeper.tasks;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.JobZooKeeper;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static java.time.Instant.now;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;
import static org.jobrunr.server.BackgroundJobServerConfiguration.DEFAULT_PAGE_REQUEST_SIZE;
import static org.jobrunr.storage.PageRequest.ascOnUpdatedAt;

public class DeleteSucceededJobsTask extends ZooKeeperTask {

    private final int pageRequestSize;
    private final AtomicInteger succeededJobsCounter;

    public DeleteSucceededJobsTask(JobZooKeeper jobZooKeeper, BackgroundJobServer backgroundJobServer) {
        super(jobZooKeeper, backgroundJobServer);
        this.pageRequestSize = DEFAULT_PAGE_REQUEST_SIZE; //backgroundJobServer.getConfiguration().succeededJobsRequestSize;
        this.succeededJobsCounter = new AtomicInteger();
    }

    @Override
    public void runTask() {
        LOGGER.debug("Looking for succeeded jobs that can go to the deleted state... ");
        succeededJobsCounter.set(0);

        final Instant updatedBefore = now().minus(serverStatus().getDeleteSucceededJobsAfter());
        Supplier<List<Job>> succeededJobsSupplier = () -> storageProvider.getJobs(SUCCEEDED, updatedBefore, ascOnUpdatedAt(pageRequestSize));

        processJobList(succeededJobsSupplier, this::deleteJobAndIncrementSucceededJobsCounter);
        if (succeededJobsCounter.get() > 0) {
            storageProvider.publishTotalAmountOfSucceededJobs(succeededJobsCounter.get());
        }
    }

    private void deleteJobAndIncrementSucceededJobsCounter(Job job) {
        succeededJobsCounter.incrementAndGet();
        job.delete("JobRunr maintenance - deleting succeeded job");
    }
}
