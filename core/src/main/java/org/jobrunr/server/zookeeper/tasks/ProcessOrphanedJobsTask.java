package org.jobrunr.server.zookeeper.tasks;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.JobZooKeeper;

import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

import static java.time.Duration.ofSeconds;
import static org.jobrunr.jobs.states.StateName.PROCESSING;
import static org.jobrunr.storage.Paging.AmountBasedList.ascOnUpdatedAt;

public class ProcessOrphanedJobsTask extends ZooKeeperTask {

    private final int pageRequestSize;

    public ProcessOrphanedJobsTask(JobZooKeeper jobZooKeeper, BackgroundJobServer backgroundJobServer) {
        super(jobZooKeeper, backgroundJobServer);
        this.pageRequestSize = backgroundJobServer.getConfiguration().getOrphanedJobsRequestSize();
    }

    @Override
    protected void runTask() {
        LOGGER.trace("Looking for orphan jobs... ");
        final Instant updatedBefore = runStartTime().minus(ofSeconds(backgroundJobServerConfiguration().getPollIntervalInSeconds()).multipliedBy(4));
        Supplier<List<Job>> orphanedJobsSupplier = () -> storageProvider.getJobList(PROCESSING, updatedBefore, ascOnUpdatedAt(pageRequestSize));
        processJobList(orphanedJobsSupplier,
                this::changeJobStateToFailedAndRunJobFilter,
                totalAmountOfOrphanedJobs -> LOGGER.debug("Found {} orphan jobs.", totalAmountOfOrphanedJobs));
    }

    private void changeJobStateToFailedAndRunJobFilter(Job job) {
        job.failed("Orphaned job", new IllegalThreadStateException("Job was too long in PROCESSING state without being updated."));
        jobFilterUtils.runOnStateAppliedFilters(job);
    }
}
